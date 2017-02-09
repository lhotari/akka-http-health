/*
 * Copyright 2017 the original author or authors.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license.  See the LICENSE file for details.
 */

package io.github.lhotari.akka.http.health

import java.lang.management._
import java.util.concurrent.ConcurrentMap
import java.util.concurrent.atomic.{AtomicBoolean, AtomicInteger}
import javax.management.{Notification, NotificationBroadcaster, NotificationFilter, NotificationListener}

import com.google.common.collect.MapMaker
import com.sun.management.{GarbageCollectionNotificationInfo, GcInfo}
import org.mockito.ArgumentCaptor
import org.mockito.Matchers._
import org.mockito.Mockito._
import org.mockito.internal.stubbing.answers.Returns
import org.scalatest.mockito.MockitoSugar
import org.scalatest.{FunSpec, Matchers}
import sun.management.MemoryNotifInfoCompositeData

import scala.collection.JavaConverters._

class LowMemoryDetectorSpec extends FunSpec with MockitoSugar with Matchers with ProcessSpawner {
  describe("low memory detector") {
    val gcBean: GarbageCollectorMXBean = createGcBean("PS Old Gen")
    val newGenGcBean: GarbageCollectorMXBean = createGcBean("New Gen")
    val memoryBean: MemoryMXBean = createMemoryBean()
    val usageMax = 10L * 1000L * 1000L * 1000L // about 10GB using 10-base
    val memoryPoolBean: MemoryPoolMXBean = createMemoryPoolBean("PS Old Gen", usageMax)

    val lowMemoryDetector = new LowMemoryDetector(occupiedHeapPercentageThreshold = 90, gcBeans = Seq(gcBean, newGenGcBean), memoryPoolBeans = Seq(memoryPoolBean), memoryBean = memoryBean)
    lowMemoryDetector.start()

    val gcListenerCapturer = ArgumentCaptor.forClass(classOf[NotificationListener])
    var gcNotificationListener: Option[NotificationListener] = None

    it("should register listener to tenure collector") {
      verify(gcBean.asInstanceOf[NotificationBroadcaster]).addNotificationListener(gcListenerCapturer.capture(), any(classOf[NotificationFilter]), any)
      gcNotificationListener = Some(gcListenerCapturer.getValue)
    }

    it("should not register listener to new collector") {
      verify(newGenGcBean.asInstanceOf[NotificationBroadcaster], never()).addNotificationListener(any(classOf[NotificationListener]), any(classOf[NotificationFilter]), any)
    }

    it("should set the threshold to the memory pool bean") {
      verify(memoryPoolBean).setCollectionUsageThreshold(9000000000L)
    }

    val memoryListenerCapturer = ArgumentCaptor.forClass(classOf[NotificationListener])
    var memoryNotificationListener: Option[NotificationListener] = None

    it("should register listener to memory bean") {
      verify(memoryBean.asInstanceOf[NotificationBroadcaster]).addNotificationListener(memoryListenerCapturer.capture(), any(classOf[NotificationFilter]), any)
      memoryNotificationListener = Some(memoryListenerCapturer.getValue)
    }

    it("should set low memory detected flag when the limit is exceeded") {
      memoryNotificationListener.get.handleNotification(createMemoryNotification(9100000000L, usageMax), null)
      assert(lowMemoryDetector.lowMemoryDetected, "Low memory should be now detected")
    }

    it("should unset low memory detected flag when memory usage goes under threshold") {
      gcNotificationListener.get.handleNotification(createGcNotification(8900000000L, usageMax), null)
      assert(!lowMemoryDetector.lowMemoryDetected, "Low memory not be flagged after healthy state")
    }

    it("should unregister listeners when it is stopped") {
      lowMemoryDetector.stop()
      verify(memoryBean.asInstanceOf[NotificationBroadcaster]).removeNotificationListener(memoryNotificationListener.get)
      verify(gcBean.asInstanceOf[NotificationBroadcaster]).removeNotificationListener(gcNotificationListener.get)
    }
  }

  private def createGcBean(memPoolName: String) = {
    val gcBean = mock[GarbageCollectorMXBean](withSettings().extraInterfaces(classOf[NotificationBroadcaster]))
    when(gcBean.getMemoryPoolNames).thenReturn(Array(memPoolName))
    gcBean
  }

  private def createMemoryPoolBean(memPoolName: String, usageMax: Long) = {
    val memoryPoolBean = mock[MemoryPoolMXBean]
    when(memoryPoolBean.getName).thenReturn(memPoolName)
    when(memoryPoolBean.isCollectionUsageThresholdSupported).thenReturn(true)
    when(memoryPoolBean.getType).thenReturn(MemoryType.HEAP)
    val memoryUsage = mock[MemoryUsage]
    when(memoryUsage.getMax).thenReturn(usageMax)
    when(memoryPoolBean.getUsage).thenReturn(memoryUsage)
    memoryPoolBean
  }

  private def createMemoryBean() = {
    mock[MemoryMXBean](withSettings().extraInterfaces(classOf[NotificationBroadcaster]))
  }

  private def createGcNotification(usage: Long, usageMax: Long, gcCause: String = ""): Notification = {
    val notification = mock[Notification]
    when(notification.getType).thenReturn(GarbageCollectionNotificationInfo.GARBAGE_COLLECTION_NOTIFICATION)
    val gcInfo = mock[GcInfo]
    val gcNotificationInfo = new GarbageCollectionNotificationInfo("gcName", "end of major GC", gcCause, gcInfo)
    when(notification.getUserData).thenAnswer(new Returns(gcNotificationInfo.toCompositeData(null).asInstanceOf[Object]))
    val memoryUsage = mock[MemoryUsage]
    when(gcInfo.getMemoryUsageAfterGc).thenReturn(Map("PS Old Gen" -> memoryUsage).asJava)
    when(memoryUsage.getUsed).thenReturn(usage)
    when(memoryUsage.getMax).thenReturn(usageMax)
    notification
  }

  private def createMemoryNotification(usage: Long, usageMax: Long): Notification = {
    val notification = mock[Notification]
    when(notification.getType).thenReturn(MemoryNotificationInfo.MEMORY_COLLECTION_THRESHOLD_EXCEEDED)
    val memoryUsage = mock[MemoryUsage]
    when(memoryUsage.getUsed).thenReturn(usage)
    when(memoryUsage.getMax).thenReturn(usageMax)
    val memoryNotificationInfo = new MemoryNotificationInfo("PS Old Gen", memoryUsage, 1)
    val userData: Object = MemoryNotifInfoCompositeData.toCompositeData(memoryNotificationInfo)
    when(notification.getUserData).thenAnswer(new Returns(userData))
    notification
  }

  for ((extraDescription: String, extraArgs: Seq[String], retval: Int) <- Seq(("", Nil, 100), (" that becomes healthy after exceeding limit", Seq("dropcache"), 101))) {
    describe(s"low memory detector in leaky application${extraDescription}") {
      for ((collectorName, jvmArgs) <- Seq(("default", Nil), ("CMS", Seq("-XX:+UseConcMarkSweepGC")), ("G1GC", Seq("-XX:+UseG1GC")))) {
        it(s"should detect a memory leak with ${collectorName} collector") {
          val result = executeInSeparateProcess(mainClassType = LeakyApplication, extraJvmOpts = jvmArgs, maxMemoryMB = 200, args = extraArgs)
          println(result.output)
          result.retval should equal(retval)
        }
      }
    }
  }
}

object LeakyApplication {
  def main(args: Array[String]): Unit = {
    val cachedropped = new AtomicBoolean(false)
    val dropcache = args.length > 0 && args(0) == "dropcache"

    val strongMap: ConcurrentMap[Integer, Array[Byte]] = new MapMaker().makeMap()

    var retval = 100

    val lowMemoryDetector = new LowMemoryDetector(occupiedHeapPercentageThreshold = 70) {
      override def handleGcNotification(info: GarbageCollectionNotificationInfo): Unit = {
        println(s"GC notification action '${info.getGcAction}' cause '${info.getGcCause}' name '${info.getGcName}' duration ${info.getGcInfo.getDuration}")
        super.handleGcNotification(info)
      }

      override protected def enteredLowMemoryState(space: MemoryUsage): Unit = {
        println("Entering low memory state")
        super.enteredLowMemoryState(space)
        if (dropcache) {
          if (cachedropped.compareAndSet(false, true)) {
            strongMap.clear()
          } else {
            System.exit(retval)
          }
        } else {
          System.exit(retval)
        }
      }

      override protected def exitedLowMemoryState(space: MemoryUsage): Unit = {
        println("Exited low memory state")
        retval = 101
        super.exitedLowMemoryState(space)
      }
    }
    lowMemoryDetector.start()

    val weakMap: ConcurrentMap[Integer, Array[Byte]] = new MapMaker().weakValues().makeMap()
    val counter = new AtomicInteger(0)
    var delay = 10
    var allocationSizeMB = 11
    while (true) {
      val bytes = Array.fill[Byte](1024 * 1024 * allocationSizeMB)(0)
      weakMap.put(counter.incrementAndGet(), bytes)
      if (counter.get() % 3 == 0) {
        strongMap.put(counter.get(), bytes)
      }
      Thread.sleep(delay)
    }
  }
}

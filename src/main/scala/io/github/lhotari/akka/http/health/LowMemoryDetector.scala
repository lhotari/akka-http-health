/*
 * Copyright 2017 the original author or authors.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license.  See the LICENSE file for details.
 */

package io.github.lhotari.akka.http.health

import java.lang.management._
import java.util.concurrent.atomic.AtomicBoolean
import java.util.logging.{Level, Logger}
import javax.management.openmbean.CompositeData
import javax.management.{Notification, NotificationBroadcaster, NotificationListener}

import com.sun.management.GarbageCollectionNotificationInfo

import scala.collection.JavaConverters._

/**
  * Detects low memory condition by using JMX API for consuming memory usage and garbage collection notification events.
  *
  * Uses <a href="http://docs.oracle.com/javase/8/docs/api/java/lang/management/MemoryPoolMXBean.html#CollectionThreshold">MemoryPoolMXBean's JMX API</a> to set
  * collection usage threshold. The listener is registered to <a href="http://docs.oracle.com/javase/8/docs/api/java/lang/management/MemoryMXBean.html">MemoryMXBean</a>.
  * <a href="https://docs.oracle.com/javase/8/docs/jre/api/management/extension/com/sun/management/GarbageCollectionNotificationInfo.html">Garbage collection notification</a> API
  * is also used. This is used to detect if memory usage drops below the threshold after crossing the threshold.
  *
  */
class LowMemoryDetector(val occupiedHeapPercentageThreshold: Int = 90, val gcBeans: Seq[GarbageCollectorMXBean] = ManagementFactory.getGarbageCollectorMXBeans.asScala.toSeq, val memoryPoolBeans: Seq[MemoryPoolMXBean] = ManagementFactory.getMemoryPoolMXBeans.asScala.toSeq, val memoryBean: MemoryMXBean = ManagementFactory.getMemoryMXBean) extends HealthChecker {
  private val LOG: Logger = Logger.getLogger(classOf[LowMemoryDetector].getName)

  private val lowMemoryDetectedFlag: AtomicBoolean = new AtomicBoolean(false)
  private val started: AtomicBoolean = new AtomicBoolean(false)

  private lazy val tenuredSpaceMemoryPoolBean = findTenuredSpaceMemoryPoolBean()

  private lazy val tenuredSpaceGcBeans: Seq[GarbageCollectorMXBean] = findTenuredSpaceGcBeans()

  val gcListener: NotificationListener = new NotificationListener {
    override def handleNotification(notification: Notification, handback: Any) = {
      if (GarbageCollectionNotificationInfo.GARBAGE_COLLECTION_NOTIFICATION == notification.getType) {
        handleGcNotification(GarbageCollectionNotificationInfo.from(notification.getUserData.asInstanceOf[CompositeData]))
      }
    }
  }

  val memoryListener: NotificationListener = new NotificationListener {
    override def handleNotification(notification: Notification, handback: scala.Any) = {
      if (MemoryNotificationInfo.MEMORY_COLLECTION_THRESHOLD_EXCEEDED == notification.getType) {
        handleMemoryCollectionThresholdExceeded(MemoryNotificationInfo.from(notification.getUserData.asInstanceOf[CompositeData]))
      }
    }
  }

  def findTenuredSpaceMemoryPoolBean(): MemoryPoolMXBean = {
    val filtered = memoryPoolBeans.filter(memoryPoolBean => memoryPoolBean.isCollectionUsageThresholdSupported && memoryPoolBean.getType == MemoryType.HEAP && isTenuredSpace(memoryPoolBean.getName))
    assert(filtered.length == 1, "Expecting a single tenured space memory pool bean.")
    filtered.head
  }

  def findTenuredSpaceGcBeans(): Seq[GarbageCollectorMXBean] = {
    gcBeans.filter(_.getMemoryPoolNames.exists(isTenuredSpace))
  }

  private def isTenuredSpace(name: String): Boolean = name.endsWith("Old Gen") || name.endsWith("Tenured Gen")

  override def start(): Unit = {
    if (started.compareAndSet(false, true)) {
      registerGcListeners()
      applyTenuredSpaceUsageThreshold()
      registerMemoryBeanListener()
    }
  }

  override def stop(): Unit = {
    if (started.compareAndSet(true, false)) {
      unregisterGcListeners()
      unregisterMemoryBeanListener()
    }
  }

  private def registerGcListeners(): Unit = {
    var listenerAdded = false
    for (gcBean <- tenuredSpaceGcBeans) {
      gcBean.asInstanceOf[NotificationBroadcaster].addNotificationListener(gcListener, null, null)
      listenerAdded = true
    }
    if (!listenerAdded) {
      LOG.warning("Cannot find GarbageCollectorMXBean for tenured space.")
    }
  }

  private def unregisterGcListeners(): Unit = {
    for (gcBean <- tenuredSpaceGcBeans) {
      gcBean.asInstanceOf[NotificationBroadcaster].removeNotificationListener(gcListener)
    }
  }

  private def applyTenuredSpaceUsageThreshold() = {
    val usageThreshold = occupiedHeapPercentageThreshold.toLong * tenuredSpaceMemoryPoolBean.getUsage.getMax / 100L
    if (LOG.isLoggable(Level.INFO)) {
      LOG.info(s"Setting threshold for ${tenuredSpaceMemoryPoolBean.getName} to ${usageThreshold} of ${tenuredSpaceMemoryPoolBean.getUsage.getMax} (${occupiedHeapPercentageThreshold}%)")
    }
    tenuredSpaceMemoryPoolBean.setCollectionUsageThreshold(usageThreshold)
  }

  private def registerMemoryBeanListener() = {
    memoryBean.asInstanceOf[NotificationBroadcaster].addNotificationListener(memoryListener, null, null)
  }

  private def unregisterMemoryBeanListener() = {
    memoryBean.asInstanceOf[NotificationBroadcaster].removeNotificationListener(memoryListener)
  }

  protected def handleGcNotification(info: GarbageCollectionNotificationInfo): Unit = {
    val spaces = info.getGcInfo.getMemoryUsageAfterGc.asScala
    for ((spaceName, space) <- spaces; if (isTenuredSpace(spaceName) && space.getMax > 0)) {
      val percentUsed: Long = 100L * space.getUsed / space.getMax
      if (percentUsed < occupiedHeapPercentageThreshold && lowMemoryDetectedFlag.compareAndSet(true, false)) {
        exitedLowMemoryState(space)
      }
    }
  }

  protected def handleMemoryCollectionThresholdExceeded(info: MemoryNotificationInfo): Unit = {
    if (lowMemoryDetectedFlag.compareAndSet(false, true)) {
      enteredLowMemoryState(info.getUsage)
    }
  }

  protected def enteredLowMemoryState(space: MemoryUsage) = {
    logMemoryStateChange("Low memory state detected.", space)
  }

  protected def exitedLowMemoryState(space: MemoryUsage) = {
    logMemoryStateChange("Memory state back to healthy.", space)
  }

  private def logMemoryStateChange(description: String, space: MemoryUsage) = {
    val msg = s"${description} tenured space usage ${space.getUsed} / ${space.getMax}"
    logErrorMessage(msg)
  }

  protected def logErrorMessage(message: String) = {
    System.err.println(message)
    LOG.warning(message)
  }

  def lowMemoryDetected = lowMemoryDetectedFlag.get()

  override def isHealthy(): Boolean = !lowMemoryDetected
}

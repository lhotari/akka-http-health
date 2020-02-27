/*
 * Copyright 2017 the original author or authors.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license.  See the LICENSE file for details.
 */

package io.github.lhotari.akka.http.health

import java.io.File

import org.mockito.Mockito._
import org.scalatest.{FunSpec, Matchers}
import org.scalatestplus.mockito.MockitoSugar

class LowDiskSpaceDetectorSpec extends FunSpec with MockitoSugar with Matchers with ProcessSpawner {
  describe("low diskspace detector") {
    val path = mock[File]
    val thresholdMB = 10
    val lowDiskSpaceDetector = new LowDiskSpaceDetector(thresholdMB = thresholdMB, path = path)

    it("should detect low diskspace") {
      when(path.getFreeSpace).thenReturn(thresholdMB * 1024 * 1024 - 1)
      lowDiskSpaceDetector.isHealthy() should equal(false)
    }

    it("should detect when diskspace is healthy") {
      when(path.getFreeSpace).thenReturn(thresholdMB * 1024 * 1024 + 1)
      lowDiskSpaceDetector.isHealthy() should equal(true)
    }
  }
}

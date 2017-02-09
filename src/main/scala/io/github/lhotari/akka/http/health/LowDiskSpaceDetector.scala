/*
 * Copyright 2017 the original author or authors.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license.  See the LICENSE file for details.
 */

package io.github.lhotari.akka.http.health

import java.io.File

class LowDiskSpaceDetector(val thresholdMB: Int = 50, val path: File = new File(".")) extends HealthChecker {
  val threholdInBytes = thresholdMB * 1024 * 1024

  override def isHealthy(): Boolean = {
    path.getFreeSpace > threholdInBytes
  }
}

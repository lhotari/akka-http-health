/*
 * Copyright 2017 the original author or authors.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license.  See the LICENSE file for details.
 */

package io.github.lhotari.akka.http.health

trait HealthChecker {
  def start(): Unit = {}

  def stop(): Unit = {}

  def isHealthy(): Boolean
}

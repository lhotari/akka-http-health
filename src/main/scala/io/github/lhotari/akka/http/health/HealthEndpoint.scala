/*
 * Copyright 2017 the original author or authors.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license.  See the LICENSE file for details.
 */

package io.github.lhotari.akka.http.health

import java.util.concurrent.atomic.AtomicBoolean

import akka.http.scaladsl.model.{ContentTypes, HttpEntity, HttpResponse, StatusCodes}
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route

import scala.concurrent.{ExecutionContext, Future}

trait HealthEndpoint {
  protected lazy val checkers = createCheckers

  protected def createCheckers = {
    Seq(new LowDiskSpaceDetector(), new LowMemoryDetector())
  }

  private lazy val successResponse: HttpResponse = createSuccessResponse

  protected def decorateResponse(response: HttpResponse) = response

  protected def createSuccessResponse = {
    decorateResponse(HttpResponse(entity = HttpEntity(ContentTypes.`text/plain(UTF-8)`, "OK")))
  }

  private lazy val errorResponse: HttpResponse = createErrorResponse

  protected def createErrorResponse = {
    decorateResponse(HttpResponse(status = StatusCodes.ServiceUnavailable))
  }

  private val started = new AtomicBoolean(false)

  def createHealthRoute(implicit executor: ExecutionContext): Route =
    get {
      path("health") {
        completeHealthCheck
      }
    }

  def completeHealthCheck(implicit executor: ExecutionContext) = {
    complete {
      Future {
        if (isHealthy()) successResponse else errorResponse
      }
    }
  }

  def start(): Unit = {
    if (started.compareAndSet(false, true)) {
      checkers.foreach(_.start())
    }
  }

  def stop(): Unit = {
    if (started.compareAndSet(true, false)) {
      checkers.foreach(_.stop())
    }
  }

  def isHealthy() = {
    start()
    checkers.forall(_.isHealthy())
  }
}

object HealthEndpoint extends HealthEndpoint {
  lazy val defaultHealthChecker = new HealthEndpoint {}

  def createDefaultHealthRoute()(implicit executor: ExecutionContext): Route = {
    defaultHealthChecker.createHealthRoute
  }
}

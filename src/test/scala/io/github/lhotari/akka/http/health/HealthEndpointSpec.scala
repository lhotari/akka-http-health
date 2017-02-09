/*
 * Copyright 2017 the original author or authors.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license.  See the LICENSE file for details.
 */

package io.github.lhotari.akka.http.health

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.testkit.ScalatestRouteTest
import org.mockito.Mockito._
import org.scalatest.mockito.MockitoSugar
import org.scalatest.{FunSpec, Matchers}

class HealthEndpointSpec extends FunSpec with Matchers with ScalatestRouteTest with MockitoSugar with HealthEndpoint {
  val mockChecker1 = mock[HealthChecker]
  val mockChecker2 = mock[HealthChecker]

  override protected def createCheckers(): Seq[HealthChecker] = Seq(mockChecker1, mockChecker2)

  describe("health endpoint") {
    it("should complete successfully when all checks are ok") {
      checkers.foreach(checker => when(checker.isHealthy()).thenReturn(true))
      Get("/health") ~> Route.seal(createHealthRoute) ~> check {
        status shouldEqual StatusCodes.OK
      }
    }

    it("should return error when a check fails") {
      when(mockChecker2.isHealthy()).thenReturn(false)
      Get("/health") ~> Route.seal(createHealthRoute) ~> check {
        status shouldEqual StatusCodes.ServiceUnavailable
      }
    }

    it("should have started each checker exactly once") {
      checkers.foreach(checker => verify(checker).start())
    }
  }

}

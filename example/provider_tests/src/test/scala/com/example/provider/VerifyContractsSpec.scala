package com.example.provider

import org.scalatest.{FunSpec, Matchers, BeforeAndAfterAll}
import org.http4s.server.Server

import com.itv.scalapact.ScalaPactVerify._

class VerifyContractsSpec extends FunSpec with Matchers with BeforeAndAfterAll {

  var runningService: Option[Server] = None

  override def beforeAll(): Unit = {
    runningService = Some(AlternateStartupApproach.startServer())
  }

  override def afterAll(): Unit = {
    runningService.foreach(AlternateStartupApproach.stopServer)
  }

  describe("Verifying Consumer Contracts") {

    it("should be able to verify it's contracts") {

      verifyPact
        .withPactSource(loadFromLocal("delivered_pacts"))
        .noSetupRequired
        .runVerificationAgainst("localhost", 8080)

    }

  }

}

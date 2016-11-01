package com.example.provider

import org.scalatest.{FunSpec, Matchers, BeforeAndAfterAll}
import org.http4s.server.Server

import com.itv.scalapact.ScalaPactVerify._

class VerifyContractsSpec extends FunSpec with Matchers with BeforeAndAfterAll {

  // Their are almost certainly nicer ways to do this.
  var runningService: Option[Server] = None

  // Before all the tests are run, we start our service, which is the real service
  // code, the only difference is that the core business logic has been replaced
  // by functional dependency injection to return known values.
  override def beforeAll(): Unit = {

    // The underscore here just denotes "whatever I'm not using the value anyway.."
    val mockLoadPeopleFunc: String => List[String] = _ =>
      List("Bob", "Fred", "Harry")

    val mockGenTokenFunc: Int => String = _ =>
      "abcABC123"

    runningService = Some(AlternateStartupApproach.startServer(mockLoadPeopleFunc, mockGenTokenFunc))
  }

  // Afterwards we need to remember to shut our service down again.
  override def afterAll(): Unit = {
    runningService.foreach(AlternateStartupApproach.stopServer)
  }

  describe("Verifying Consumer Contracts") {

    it("should be able to verify it's contracts") {

      verifyPact
        .withPactSource(loadFromLocal("delivered_pacts"))
        .noSetupRequired // We did the setup in the beforeAll() function
        .runVerificationAgainst("localhost", 8080)

    }

  }

}

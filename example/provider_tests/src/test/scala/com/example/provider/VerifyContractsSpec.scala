package com.example.provider

import cats.effect.IO
import org.scalatest.{BeforeAndAfterAll, FunSpec, Matchers}
import org.http4s.server.Server
import com.itv.scalapact.ScalaPactVerify._
import com.itv.scalapactcore.verifier.Verifier.ProviderStateResult

class VerifyContractsSpec extends FunSpec with Matchers with BeforeAndAfterAll {

  import com.itv.scalapact.circe09._
  import com.itv.scalapact.http4s18._

  // Their are almost certainly nicer ways to do this.
  var runningService: Option[Server[IO]] = None

  // Before all the tests are run, we start our service, which is the real service
  // code, the only difference is that the core business logic has been replaced
  // by functional dependency injection to return known values.
  override def beforeAll(): Unit = {

    // The underscore here just denotes "whatever I'm not using the value anyway.."
    val mockLoadPeopleFunc: String => List[String] = _ => List("Bob", "Fred", "Harry")

    val mockGenTokenFunc: Int => String = _ => "abcABC123"

    runningService = Some(AlternateStartupApproach.startServer(mockLoadPeopleFunc, mockGenTokenFunc))
  }

  // Afterwards we need to remember to shut our service down again.
  override def afterAll(): Unit =
    runningService.foreach(p => p.shutdownNow())

  describe("Verifying Consumer Contracts") {

    it("should be able to verify it's contracts") {

      verifyPact
        .withPactSource(loadFromLocal("delivered_pacts"))
        .setupProviderState("given") {
          case "Results: Bob, Fred, Harry" =>
            val newHeader = "Pact" -> "modifiedRequest"
            ProviderStateResult(true, req => req.copy(headers = Option(req.headers.fold(Map(newHeader))(_ + newHeader))))
        }
        .runVerificationAgainst("localhost", 8080)

    }

  }

}

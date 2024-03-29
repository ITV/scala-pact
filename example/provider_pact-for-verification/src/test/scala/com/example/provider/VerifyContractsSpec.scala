package com.example.provider

import cats.effect.unsafe.implicits.global
import org.scalatest.BeforeAndAfterAll
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers
import com.itv.scalapact.PactVerifySuite
import com.itv.scalapact.shared.{
  ConsumerVersionSelector,
  PactBrokerAuthorization,
  ProviderStateResult,
  PendingPactSettings
}

import scala.concurrent.duration._

class VerifyContractsSpec extends AnyFunSpec with Matchers with BeforeAndAfterAll with PactVerifySuite {
  val serverAllocated =
    AlternateStartupApproach
      .serverResource(_ => List("Bob", "Fred", "Harry"), _ => "abcABC123")
      .allocated
      .unsafeRunSync()

  override def beforeAll(): Unit =
    ()

  override def afterAll(): Unit =
    serverAllocated._2.unsafeRunSync()

  describe("Verifying Consumer Contracts") {
    it("should be able to verify it's contracts") {
      val consumer = ConsumerVersionSelector(
        "example",
        latest = true
      ) // This should fetch all the latest pacts of consumer with tag example

      verifyPact
        .withPactSource(
          pactBrokerWithVersionSelectors(
            "https://test.pact.dius.com.au",
            "scala-pact-provider",
            List(consumer),
            List(),
            PendingPactSettings.PendingDisabled,
            None,
            //again, these are publicly known creds for a test pact-broker
            PactBrokerAuthorization(
              pactBrokerCredentials = ("dXfltyFMgNOFZAxr8io9wJ37iUpY42M", "O5AIZWxelWbLvqMd8PkAVycBJh2Psyg1"),
              ""
            ),
            Some(5.seconds)
          )
        )
        .setupProviderState("given") { case "Results: Bob, Fred, Harry" =>
          val newHeader = "Pact" -> "modifiedRequest"
          ProviderStateResult(true, req => req.copy(headers = Option(req.headers.fold(Map(newHeader))(_ + newHeader))))
        }
        .runVerificationAgainst("localhost", 8080, 10.seconds)
    }
  }

}

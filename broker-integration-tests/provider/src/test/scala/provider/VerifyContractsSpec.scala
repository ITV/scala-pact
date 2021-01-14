package provider

import org.scalatest.{BeforeAndAfterAll, FunSpec, Matchers}
import com.itv.scalapact.PactVerifySuite
import com.itv.scalapact.shared.{BrokerPublishData, ConsumerVersionSelector, PactBrokerAuthorization, PendingPactSettings}
import scalaj.http._

import scala.concurrent.duration._

class VerifyContractsSpec extends FunSpec with Matchers with BeforeAndAfterAll with PactVerifySuite {
  val serverAllocated =
    AlternateStartupApproach.serverResource.allocated.unsafeRunSync()

  val providerVersion = "0.0.1"
  val providerTag = "provider-tag"
  val providerName = "scala-pact-integration-test-provider"
  val brokerAuth = PactBrokerAuthorization(pactBrokerCredentials = ("dXfltyFMgNOFZAxr8io9wJ37iUpY42M", "O5AIZWxelWbLvqMd8PkAVycBJh2Psyg1"), "")

  val tagRequest = {
    val authHeader = brokerAuth.map(_.asHeader).get
    Http(
      s"https://test.pact.dius.com.au/pacticipants/$providerName/versions/$providerVersion/tags/$providerTag"
    ).header(authHeader._1, authHeader._2)
  }
  override def beforeAll(): Unit = {
    val _ = tagRequest.method("DELETE").asString
    ()
  }

  override def afterAll(): Unit = {
    serverAllocated._2.unsafeRunSync()
  }


  describe("Verifying Consumer Contracts") {
    it("should be able to verify it's contracts") {
      val consumers = List(
        ConsumerVersionSelector("test", latest = true)
      )

      verifyPact
        .withPactSource(
          pactBrokerWithVersionSelectors(
            "https://test.pact.dius.com.au",
            providerName,
            consumers,
            List(providerTag),
            pendingPactSettings = PendingPactSettings.PendingEnabled,
            Some(BrokerPublishData(providerVersion, None)),
            //again, these are publicly known creds for a test pact-broker
            brokerAuth,
            Some(5.seconds)
          )
        )
        .noSetupRequired
        .runVerificationAgainst("localhost", 8080, 10.seconds)

      val fetchTag = tagRequest.asString

      //check tag exists in broker
      fetchTag.code shouldBe 204
    }
  }

}

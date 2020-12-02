package provider

import org.scalatest.{BeforeAndAfterAll, FunSpec, Matchers}
import com.itv.scalapact.PactVerifySuite
import com.itv.scalapact.shared.{ConsumerVersionSelector, PactBrokerAuthorization, ProviderStateResult, PendingPactSetttings}

import scala.concurrent.duration._

class VerifyContractsSpec extends FunSpec with Matchers with BeforeAndAfterAll with PactVerifySuite {
  val serverAllocated =
    AlternateStartupApproach.serverResource.allocated.unsafeRunSync()

  override def beforeAll(): Unit = {
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
            "scala-pact-pending-test-provider",
            consumers,
            List("master"),
            pendingPactSettings = PendingPactSettings.PendingEnabled,
            None,
            //again, these are publicly known creds for a test pact-broker
            PactBrokerAuthorization(pactBrokerCredentials = ("dXfltyFMgNOFZAxr8io9wJ37iUpY42M", "O5AIZWxelWbLvqMd8PkAVycBJh2Psyg1"), ""),
            Some(5.seconds)
          )
        )
        .noSetupRequired
        .runVerificationAgainst("localhost", 8080, 10.seconds)
    }
  }

}

package com.itv.scalapact

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.client.WireMock._
import com.github.tomakehurst.wiremock.core.WireMockConfiguration._
import com.itv.scalapact.ScalaPactVerify._
import org.scalatest.BeforeAndAfterAll

import scala.concurrent.duration._
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers

class ExampleVerification extends AnyFunSpec with Matchers with BeforeAndAfterAll {

  import com.itv.scalapact.json._
  import com.itv.scalapact.http._

  val samplePact = """
  {
    "provider" : {
      "name" : "Me"
    },
    "consumer" : {
      "name" : "One of my consumers"
    },
    "interactions" : [
      {
        "description" : "Simple example",
        "request" : {
          "method" : "GET",
          "path" : "/example"
        },
        "response" : {
          "status" : 200,
          "body" : "Success"
        }
      },
      {
        "description" : "A strict example",
        "request" : {
          "method" : "GET",
          "path" : "/strict"
        },
        "response" : {
          "status" : 200,
          "body" : ["red", "blue"]
        }
      }
    ]
  }
  """

  private val wireMockServer = new WireMockServer(wireMockConfig().port(0))

  private def port(): Int = wireMockServer.port

  override def beforeAll(): Unit = {

    wireMockServer.start()

    WireMock.configureFor("localhost", port())

    val response1 = aResponse().withStatus(200).withBody("Success")
    val response2 = aResponse().withStatus(200).withBody("[\"blue\", \"red\"]")

    wireMockServer.stubFor(
      get(urlEqualTo("/example")).willReturn(response1)
    )

    wireMockServer.stubFor(
      get(urlEqualTo("/strict")).willReturn(response2)
    )

    ()
  }

  override def afterAll(): Unit =
    wireMockServer.stop()

  describe("Verification of pacts using provider tests") {

    it("should be able to verify a simple contract") {

      verifyPact
        .withPactSource(pactAsJsonString(samplePact))
        .noSetupRequired
        .runVerificationAgainst(port(), 5.seconds)

    }

    it("should verify a permissive pact") {

      verifyPact
        .withPactSource(pactAsJsonString(samplePact))
        .noSetupRequired
        .runVerificationAgainst(port())

    }

    it("should fail to verify a pact, strictly verified, that does not conform to the Pact spec.") {

      intercept[ScalaPactVerifyFailed] {
        verifyPact
          .withPactSource(pactAsJsonString(samplePact))
          .noSetupRequired
          .runStrictVerificationAgainst(port())
      }

    }

  }

}

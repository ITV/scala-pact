package com.itv.scalapact

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.{MappingBuilder, WireMock}
import com.github.tomakehurst.wiremock.client.WireMock._
import com.github.tomakehurst.wiremock.core.WireMockConfiguration._
import org.scalatest.{FunSpec, Matchers, BeforeAndAfterAll}

import ScalaPactVerify._

class ExampleVerification extends FunSpec with Matchers with BeforeAndAfterAll {

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
        "description" : "Just an example",
        "request" : {
          "method" : "GET",
          "path" : "/example"
        },
        "response" : {
          "status" : 200,
          "body" : "Success"
        }
      }
    ]
  }
  """

  val wireMockServer = new WireMockServer(wireMockConfig().port(1234))

  override def beforeAll(): Unit = {

    wireMockServer.start()

    WireMock.configureFor("localhost", 1234)

    val response = aResponse().withStatus(200).withBody("Success")

    wireMockServer.stubFor(
      get(urlEqualTo("/example")).willReturn(response)
    )
  }


  override def afterAll(): Unit = {
    wireMockServer.stop()
  }

  describe("Verification of pacts using provider tests") {

    it("should be able to verify a simple contract") {

     verifyPact
       .withPactSource(pactContractString(samplePact)) // OR .withPactSource(pactBroker("url")) OR .withPactSource(pactBroker("url").withContractVersion("1.0.0"))
       .noSetupRequired
       .runVerificationAgainst("localhost", 1234)

    }

  }

}

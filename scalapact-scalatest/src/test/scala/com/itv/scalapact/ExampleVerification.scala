package com.itv.scalapact

import org.scalatest.{FunSpec, Matchers}

import ScalaPactVerify._

class ExampleVerification extends FunSpec with Matchers {

  def beforeAll: Unit = {
    // start service
  }

  def afterAll: Unit = {
    // start service
  }

  describe("Verification of pacts using provider tests") {

    it("should be able to verify a simple contract") {

      verifyPact
        .between("My Consumer")
        .and("My Provider")
        .withPactSource(directory("some/directory/pacts/")) // OR .withPactSource(pactBroker("url")) OR .withPactSource(pactBroker("url").withContractVersion("1.0.0"))
        .setupProviderState {
          case "document 1234" => true
        }
        .runVerificationAgainst("http://localhost:1234")

    }

  }

}

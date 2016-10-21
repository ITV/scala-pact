package com.itv.scalapact

import org.scalatest.{FunSpec, Matchers}

import ScalaPactVerify._

class ExampleVerification extends FunSpec with Matchers {

  describe("Verification of pacts using provider tests") {

    it("should be able to verify a simple contract") {

      // This test is here for example purposes only, it will not work in the context.

//      verifyPact
//        .between("My Consumer")
//        .and("My Provider")
//        .withPactSource(directory("target/pacts")) // OR .withPactSource(pactBroker("url")) OR .withPactSource(pactBroker("url").withContractVersion("1.0.0"))
//        .setupProviderState("document 1234") { key =>
//            // Do some setup work
//            true
//        }
//        .runVerificationAgainst(1234)

    }

  }

}

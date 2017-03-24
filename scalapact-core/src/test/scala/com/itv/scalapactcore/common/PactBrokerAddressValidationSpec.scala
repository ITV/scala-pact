package com.itv.scalapactcore.common

import org.scalatest.{FunSpec, Matchers}

class PactBrokerAddressValidationSpec extends FunSpec with Matchers {

  describe("Validating pact broker urls") {

    it("reject bogus addresses") {

      PactBrokerAddressValidation.checkPactBrokerAddress("") shouldEqual Left("Pact broker address not set, you need to add the following line to you SBT file: \npactBrokerAddress := \"http://pactbroker.myserver.com\"")
      PactBrokerAddressValidation.checkPactBrokerAddress("fish") shouldEqual Left("Pact broker address does not appear to be valid, should start with 'http(s)' protocol.")
      PactBrokerAddressValidation.checkPactBrokerAddress("httpfish") shouldEqual Left("Pact broker address does not appear to be valid, should be of form https://my.broker-address.com or http://localhost:8080")

    }

    it("should accept ok addresses") {

      PactBrokerAddressValidation.checkPactBrokerAddress("http://localhost:5000") shouldEqual Right(ValidPactBrokerAddress("http://localhost:5000"))
      PactBrokerAddressValidation.checkPactBrokerAddress("https://my.broker-address.com") shouldEqual Right(ValidPactBrokerAddress("https://my.broker-address.com"))

    }

  }

}

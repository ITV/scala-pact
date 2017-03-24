package com.itv.scalapactcore.common

object PactBrokerAddressValidation {

  lazy val checkPactBrokerAddress: String => Either[String, ValidPactBrokerAddress] = address =>
    address match {
      case a: String if a.isEmpty =>
        Left("Pact broker address not set, you need to add the following line to you SBT file: \npactBrokerAddress := \"http://pactbroker.myserver.com\"")

      case a: String if !a.startsWith("http") =>
        Left("Pact broker address does not appear to be valid, should start with 'http(s)' protocol.")

      case a: String if !a.matches("""^(http)(s?)(:\/\/)([A-Za-z0-9-\.\/]+)(:[0-9]+)?$""") =>
        Left("Pact broker address does not appear to be valid, should be of form https://my.broker-address.com or http://localhost:8080")

      case a: String =>
        Right(ValidPactBrokerAddress(a))
    }

}

case class ValidPactBrokerAddress(address: String)
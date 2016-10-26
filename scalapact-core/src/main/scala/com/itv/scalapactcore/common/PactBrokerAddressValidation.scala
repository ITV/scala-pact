package com.itv.scalapactcore.common

object PactBrokerAddressValidation {

  lazy val checkPactBrokerAddress: String => Either[String, String] = address =>
    address match {
      case a: String if a.isEmpty =>
        Left("Pact broker address not set, you need to add the following line to you SBT file: \npactBrokerAddress := \"http://pactbroker.myserver.com\"")

      case a: String if !a.startsWith("http") =>
        Left("Pact broker address does not appear to be valid, should start with 'http(s)' protocol.")

      case a: String if !a.matches("^(http)(s?)(://)([A-Za-z0-9-\\./]+)([A-Za-z0-9]$)") =>
        Left("Pact broker address does not appear to be valid, should be of form: http://my.broker-address.com")

      case a: String =>
        Right(a)
    }

}

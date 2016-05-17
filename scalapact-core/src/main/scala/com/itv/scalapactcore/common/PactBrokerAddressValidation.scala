package com.itv.scalapactcore.common

import scalaz._
import Scalaz._

object PactBrokerAddressValidation {

  lazy val checkPactBrokerAddress: String => String \/ String = address =>
    address match {
      case a: String if a.isEmpty =>
        "Pact broker address not set, you need to add the following line to you SBT file: \npactBrokerAddress := \"http://pactbroker.myserver.com\"".left

      case a: String if !a.startsWith("http") =>
        "Pact broker address does not appear to be valid, should start with 'http(s)' protocol.".left

      case a: String if !a.matches("^(http)(s?)(://)([A-Za-z0-9-\\./]+)([A-Za-z0-9]$)") =>
        "Pact broker address does not appear to be valid, should be of form: http://my.broker-address.com".left

      case a: String =>
        a.right
    }

}

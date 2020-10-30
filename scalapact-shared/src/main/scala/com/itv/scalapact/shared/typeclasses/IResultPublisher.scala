package com.itv.scalapact.shared.typeclasses

import com.itv.scalapact.shared.{PactBrokerAuthorization, PactVerifyResult}

trait IResultPublisher {
  def publishResults(
      pactVerifyResults: List[PactVerifyResult],
      brokerPublishData: BrokerPublishData,
      pactBrokerAuthorization: Option[PactBrokerAuthorization]
  ): Unit
}

case class BrokerPublishData(providerVersion: String, buildUrl: Option[String])

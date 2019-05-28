package com.itv.scalapact.shared

trait IResultPublisher {
  def publishResults(
      pactVerifyResults: List[PactVerifyResult],
      brokerPublishData: BrokerPublishData,
      pactBrokerAuthorization: Option[PactBrokerAuthorization]
  )(implicit sslContextMap: SslContextMap): Unit
}

case class BrokerPublishData(providerVersion: String, buildUrl: Option[String])

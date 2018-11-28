package com.itv.scalapact.shared

trait IResultPublisher {
  def publishResults(pactVerifyResults: List[PactVerifyResult], brokerPublishData: BrokerPublishData, pactBrokerCredentials: Option[BasicAuthenticationCredentials])(implicit sslContextMap: SslContextMap): Unit
}

case class BrokerPublishData(providerVersion: String, buildUrl: Option[String])

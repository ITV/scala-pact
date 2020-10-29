package com.itv.scalapact.shared

import com.itv.scalapact.shared.ProviderStateResult.SetupProviderState

import scala.concurrent.duration.Duration

sealed trait PactVerifySettings extends Product with Serializable {
  def providerStates: SetupProviderState
}

final case class LocalPactVerifySettings(providerStates: SetupProviderState) extends PactVerifySettings

sealed trait BrokerPactVerifySettings extends PactVerifySettings {
  def pactBrokerAddress: String
  def providerName: String
  def pactBrokerAuthorization: Option[PactBrokerAuthorization]
  def pactBrokerClientTimeout: Option[Duration]
  def sslContextName: Option[String]
}

final case class PactsForVerificationSettings(providerStates: SetupProviderState,
                                              pactBrokerAddress: String,
                                              providerName: String,
                                              consumerVersionSelectors: List[ConsumerVersionSelector],
                                              providerVersionTags: List[String],
                                              includePendingStatus: Boolean,
                                              pactBrokerAuthorization: Option[PactBrokerAuthorization],
                                              pactBrokerClientTimeout: Option[Duration],
                                              sslContextName: Option[String]) extends BrokerPactVerifySettings

sealed trait PrePactsForVerificationSettings extends BrokerPactVerifySettings

final case class LatestConsumerVerifySettings(providerStates: SetupProviderState,
                                              pactBrokerAddress: String,
                                              providerName: String,
                                              consumerNames: List[String],
                                              pactBrokerAuthorization: Option[PactBrokerAuthorization],
                                              pactBrokerClientTimeout: Option[Duration],
                                              sslContextName: Option[String]) extends PrePactsForVerificationSettings

final case class TaggedConsumerVerifySettings(providerStates: SetupProviderState,
                                              pactBrokerAddress: String,
                                              providerName: String,
                                              taggedConsumerNames: List[TaggedConsumer],
                                              pactBrokerAuthorization: Option[PactBrokerAuthorization],
                                              pactBrokerClientTimeout: Option[Duration],
                                              sslContextName: Option[String]) extends PrePactsForVerificationSettings

final case class VersionedConsumerVerifySettings(providerStates: SetupProviderState,
                              pactBrokerAddress: String,
                              providerName: String,
                              versionedConsumerNames: List[VersionedConsumer],
                              pactBrokerAuthorization: Option[PactBrokerAuthorization],
                              pactBrokerClientTimeout: Option[Duration],
                              sslContextName: Option[String]) extends PrePactsForVerificationSettings

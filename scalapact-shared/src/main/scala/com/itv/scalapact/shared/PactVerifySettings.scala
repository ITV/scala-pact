package com.itv.scalapact.shared

import java.time.OffsetDateTime

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

final case class PactsForVerificationSettings(
    providerStates: SetupProviderState,
    pactBrokerAddress: String,
    providerName: String,
    consumerVersionSelectors: List[ConsumerVersionSelector],
    providerVersionTags: List[String],
    includePendingStatus: Boolean,
    includeWipPactsSince: Option[OffsetDateTime],
    pactBrokerAuthorization: Option[PactBrokerAuthorization],
    pactBrokerClientTimeout: Option[Duration],
    sslContextName: Option[String]
) extends BrokerPactVerifySettings

final case class ConsumerVerifySettings(
    providerStates: SetupProviderState,
    pactBrokerAddress: String,
    providerName: String,
    versionedConsumerNames: List[VersionedConsumer],
    pactBrokerAuthorization: Option[PactBrokerAuthorization],
    pactBrokerClientTimeout: Option[Duration],
    sslContextName: Option[String]
) extends BrokerPactVerifySettings

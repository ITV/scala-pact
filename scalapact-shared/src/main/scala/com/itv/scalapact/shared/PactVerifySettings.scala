package com.itv.scalapact.shared

import com.itv.scalapact.shared.ProviderStateResult.SetupProviderState

import scala.concurrent.duration.Duration

case class PactVerifySettings(providerStates: SetupProviderState,
                              pactBrokerAddress: String,
                              projectVersion: String,
                              providerName: String,
                              consumerNames: List[String],
                              taggedConsumerNames: List[TaggedConsumer],
                              versionedConsumerNames: List[VersionedConsumer],
                              consumerVersionSelectors: List[ConsumerVersionSelector],
                              providerVersionTags: List[String],
                              pactBrokerAuthorization: Option[PactBrokerAuthorization],
                              pactBrokerClientTimeout: Option[Duration],
                              sslContextName: Option[String])

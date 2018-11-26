package com.itv.scalapact.shared

import com.itv.scalapact.shared.ProviderStateResult.SetupProviderState

case class PactVerifySettings(providerStates: SetupProviderState,
                              pactBrokerAddress: String,
                              projectVersion: String,
                              providerName: String,
                              consumerNames: List[String],
                              taggedConsumerNames: List[TaggedConsumer],
                              versionedConsumerNames: List[VersionedConsumer])

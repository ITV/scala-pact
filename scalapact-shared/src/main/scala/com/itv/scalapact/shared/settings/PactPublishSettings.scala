package com.itv.scalapact.shared.settings

import com.itv.scalapact.shared.PactBrokerAuthorization

import scala.concurrent.duration.Duration

final case class PactPublishSettings(
    pactBrokerAddress: String,
    providerBrokerPublishMap: Map[String, String],
    projectVersion: String,
    pactContractVersion: String,
    allowSnapshotPublish: Boolean,
    tagsToPublishWith: List[String],
    pactBrokerAuthorization: Option[PactBrokerAuthorization],
    pactBrokerClientTimeout: Duration,
    sslContextName: Option[String]
)

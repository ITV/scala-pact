package com.itv.scalapact.plugin.shared

import com.itv.scalapact.shared.PactBrokerAuthorization
import com.itv.scalapact.shared.http.IScalaPactHttpClientBuilder
import com.itv.scalapact.shared.json.{IPactReader, IPactWriter}
import com.itv.scalapactcore.common.PactBrokerClient

import scala.concurrent.duration.Duration

object ScalaPactDeleteTagCommand {

  def doDeleteTag(
                   deleteTagSettings: DeleteTagSettings,
                   pactBrokerAddress: String,
                   pactBrokerAuthorization: Option[PactBrokerAuthorization],
                   pactBrokerClientTimeout: Duration,
                   sslContextName: Option[String]
                 )(implicit pactReader: IPactReader, pactWriter: IPactWriter, httpClientBuilder: IScalaPactHttpClientBuilder): Unit = {
    val brokerClient = new PactBrokerClient
    brokerClient.deletePacticipantTag(
      pactBrokerAddress,
      deleteTagSettings.pacticipant,
      deleteTagSettings.version,
      deleteTagSettings.tag,
      pactBrokerAuthorization,
      pactBrokerClientTimeout,
      sslContextName
    )
  }
}

final case class DeleteTagSettings(pacticipant: String, version: String, tag: String)

package com.itv.scalapact.plugin.shared

import com.itv.scalapact.shared.utils.ColourOutput._
import com.itv.scalapact.shared._
import com.itv.scalapact.shared.http.IScalaPactHttpClientBuilder
import com.itv.scalapact.shared.json.{IPactReader, IPactWriter}
import com.itv.scalapact.shared.utils.PactLogger
import com.itv.scalapactcore.publisher.{PublishFailed, PublishResult, PublishSuccess, Publisher}

import scala.concurrent.duration._

object ScalaPactPublishCommand {

  def doPactPublish(
      scalaPactSettings: ScalaPactSettings,
      pactBrokerAddress: String,
      providerBrokerPublishMap: Map[String, String],
      projectVersion: String,
      pactContractVersion: String,
      allowSnapshotPublish: Boolean,
      tagsToPublishWith: Seq[String],
      pactBrokerAuthorization: Option[PactBrokerAuthorization],
      pactBrokerClientTimeout: Duration,
      sslContextName: Option[String]
  )(implicit pactReader: IPactReader, pactWriter: IPactWriter, httpClientBuilder: IScalaPactHttpClientBuilder): Unit = {

    PactLogger.message("*************************************".white.bold)
    PactLogger.message("** ScalaPact: Publishing Contracts **".white.bold)
    PactLogger.message("*************************************".white.bold)

    val versionToPublishAs = if (pactContractVersion.isEmpty) projectVersion else pactContractVersion

    if (versionToPublishAs.contains("SNAPSHOT") && !allowSnapshotPublish) {
      PactLogger.error("Snapshot pact file publishing not permitted".red.bold)
      PactLogger.error("Publishing of pact contracts against snapshot versions is not allowed by default.".yellow)
      PactLogger.error("Pact broker does not cope well with snapshot contracts.".yellow)
      PactLogger.error("To enable this feature, add \"allowSnapshotPublish := true\" to your pact.sbt file.".yellow)
    } else {
      val publishSettings = PactPublishSettings(
        pactBrokerAddress,
        providerBrokerPublishMap,
        versionToPublishAs,
        pactContractVersion,
        allowSnapshotPublish,
        tagsToPublishWith.toList,
        pactBrokerAuthorization,
        pactBrokerClientTimeout,
        sslContextName
      )
      val publishResults = Publisher.apply.publishPacts(publishSettings, scalaPactSettings)
      evaluatePublishResults(publishResults)
    }
  }

  private def evaluatePublishResults(publishResults: List[PublishResult]): Unit = {
    publishResults.foreach {
      case result: PublishSuccess => PactLogger.message(result.renderAsString)
      case result: PublishFailed  => PactLogger.error(result.renderAsString)
    }

    if (!publishResults.forall(_.isSuccess)) exitFailure()
  }

  private def exitFailure(): Unit = sys.exit(1)
}

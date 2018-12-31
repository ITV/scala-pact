package com.itv.scalapact.plugin.shared

import com.itv.scalapact.shared.ColourOutput._
import com.itv.scalapact.shared.typeclasses.{IPactReader, IPactWriter, IScalaPactHttpClient}
import com.itv.scalapact.shared.{BasicAuthenticationCredentials, ConfigAndPacts, PactLogger, ScalaPactSettings}
import com.itv.scalapactcore.common.LocalPactFileLoader

object ScalaPactPublishCommand {

  def doPactPublish[F[_]](
      scalaPactSettings: ScalaPactSettings,
      pactBrokerAddress: String,
      providerBrokerPublishMap: Map[String, String],
      projectVersion: String,
      pactContractVersion: String,
      allowSnapshotPublish: Boolean,
      tagsToPublishWith: Seq[String],
      basicAuthenticationCredentials: (String, String)
  )(implicit pactReader: IPactReader, pactWriter: IPactWriter, httpClient: IScalaPactHttpClient[F]): Unit = {
    import Publisher._

    PactLogger.message("*************************************".white.bold)
    PactLogger.message("** ScalaPact: Publishing Contracts **".white.bold)
    PactLogger.message("*************************************".white.bold)

    val versionToPublishAs = if (pactContractVersion.isEmpty) projectVersion else pactContractVersion
    val credentials =
      if (basicAuthenticationCredentials._1.isEmpty || basicAuthenticationCredentials._2.isEmpty) None
      else Some(BasicAuthenticationCredentials(basicAuthenticationCredentials._1, basicAuthenticationCredentials._2))

    if (versionToPublishAs.contains("SNAPSHOT") && !allowSnapshotPublish) {
      PactLogger.error("Snapshot pact file publishing not permitted".red.bold)
      PactLogger.error("Publishing of pact contracts against snapshot versions is not allowed by default.".yellow)
      PactLogger.error("Pact broker does not cope well with snapshot contracts.".yellow)
      PactLogger.error("To enable this feature, add \"allowSnapshotPublish := true\" to your pact.sbt file.".yellow)
    } else {
      val configAndPactFiles =
        LocalPactFileLoader.loadPactFiles(pactReader)(false)(scalaPactSettings.giveOutputPath)(scalaPactSettings)

      // Publish all to main broker
      publishToBroker(httpClient.doRequestSync, pactBrokerAddress, versionToPublishAs, tagsToPublishWith, credentials)(
        pactWriter
      )(
        configAndPactFiles
      ).foreach(r => PactLogger.message(r.renderAsString))

      // Publish to other specified brokers
      configAndPactFiles.pacts.foreach { pactContract =>
        providerBrokerPublishMap.get(pactContract.provider.name).foreach { broker =>
          publishToBroker(httpClient.doRequestSync, broker, versionToPublishAs, tagsToPublishWith, credentials)(
            pactWriter
          )(
            ConfigAndPacts(scalaPactSettings, List(pactContract))
          ).foreach(r => PactLogger.message(r.renderAsString))
        }
      }

    }
  }
}

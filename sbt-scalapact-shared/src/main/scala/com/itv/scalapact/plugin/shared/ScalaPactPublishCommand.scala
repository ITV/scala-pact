package com.itv.scalapact.plugin.shared

import com.itv.scalapact.shared.ColourOutput._
import com.itv.scalapact.shared.typeclasses.{IPactReader, IPactWriter, IScalaPactHttpClient}
import com.itv.scalapact.shared._
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
      pactBrokerAuthorization: Option[PactBrokerAuthorization]
  )(implicit pactReader: IPactReader, pactWriter: IPactWriter, httpClient: IScalaPactHttpClient[F]): Unit = {
    import Publisher._

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
      val configAndPactFiles =
        LocalPactFileLoader.loadPactFiles(pactReader)(false)(scalaPactSettings.giveOutputPath)(scalaPactSettings)

      // Publish all to main broker
      val mainPublishResults: List[PublishResult] = publishToBroker(
        httpClient.doRequestSync(_, scalaPactSettings.giveClientTimeout),
        pactBrokerAddress,
        versionToPublishAs,
        tagsToPublishWith,
        pactBrokerAuthorization
      )(pactWriter)(configAndPactFiles)

      // Publish to other specified brokers
      val otherPublishResults: List[PublishResult] = for {
        pactContract <- configAndPactFiles
        broker       <- providerBrokerPublishMap.get(pactContract.provider.name).toList
        publishResult <- publishToBroker(
          httpClient.doRequestSync(_, scalaPactSettings.giveClientTimeout),
          broker,
          versionToPublishAs,
          tagsToPublishWith,
          pactBrokerAuthorization
        )(pactWriter)(List(pactContract))
      } yield publishResult

      evaluatePublishResults(mainPublishResults ++ otherPublishResults)
    }
  }

  private def evaluatePublishResults(publishResults: List[PublishResult]): Unit = {
    publishResults.foreach {
      case result: PublishSuccess => PactLogger.message(result.renderAsString)
      case result: PublishFailed  => PactLogger.error(result.renderAsString)
    }

    val noErrors = publishResults.collectFirst { case _: PublishFailed => () }.isEmpty

    if (noErrors) exitSuccess()
    else exitFailure()
  }

  private def exitSuccess(): Unit = sys.exit(0)

  private def exitFailure(): Unit = sys.exit(1)
}

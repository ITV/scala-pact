package com.itv.scalapact.plugin.shared

import com.itv.scalapact.shared.utils.ColourOutput._
import com.itv.scalapact.shared._
import com.itv.scalapact.shared.ProviderStateResult.SetupProviderState
import com.itv.scalapact.shared.http.IScalaPactHttpClientBuilder
import com.itv.scalapact.shared.json.{IPactReader, IPactWriter}
import com.itv.scalapact.shared.utils.PactLogger
import com.itv.scalapactcore.verifier.Verifier

import scala.concurrent.duration._

object ScalaPactVerifyCommand {

  def doPactVerify(
      scalaPactSettings: ScalaPactSettings,
      providerStates: Seq[(String, SetupProviderState)],
      providerStateMatcher: PartialFunction[String, ProviderStateResult],
      pactBrokerAddress: String,
      providerName: String,
      consumerNames: Seq[String],
      versionedConsumerNames: Seq[(String, String)],
      taggedConsumerNames: Seq[(String, Seq[String])],
      consumerVersionSelectors: Seq[ConsumerVersionSelector],
      providerVersionTags: Seq[String],
      pactBrokerAuthorization: Option[PactBrokerAuthorization],
      pactBrokerClientTimeout: Duration,
      sslContextName: Option[String],
      includePendingStatus: Boolean
  )(implicit pactReader: IPactReader, pactWriter: IPactWriter, httpClientBuilder: IScalaPactHttpClientBuilder): Unit = {
    PactLogger.message("*************************************".white.bold)
    PactLogger.message("** ScalaPact: Running Verifier     **".white.bold)
    PactLogger.message("*************************************".white.bold)

    val combinedPactStates = combineProviderStatesIntoTotalFunction(providerStates, providerStateMatcher)

    val pactVerifySettings = {
      if (scalaPactSettings.localPactFilePath.isDefined)
        LocalPactVerifySettings(combinedPactStates)
      else if (consumerVersionSelectors.nonEmpty)
        PactsForVerificationSettings(
          combinedPactStates,
          pactBrokerAddress,
          providerName,
          consumerVersionSelectors.toList,
          providerVersionTags.toList,
          scalaPactSettings.enablePending.getOrElse(includePendingStatus),
          pactBrokerAuthorization,
          Some(pactBrokerClientTimeout),
          sslContextName
        )
      else {
        val versionedConsumers =
          consumerNames.map(VersionedConsumer.fromName) ++
            versionedConsumerNames.map(v => VersionedConsumer(v._1, v._2)) ++
            taggedConsumerNames.flatMap(t => VersionedConsumer.fromNameAndTags(t._1, t._2.toList))
        ConsumerVerifySettings(
          combinedPactStates,
          pactBrokerAddress,
          providerName,
          versionedConsumers.toList,
          pactBrokerAuthorization,
          Some(pactBrokerClientTimeout),
          sslContextName
        )
      }
    }

    val successfullyVerified = Verifier.apply.verify(pactVerifySettings, scalaPactSettings)

    if (successfullyVerified) sys.exit(0) else sys.exit(1)

  }

  def combineProviderStatesIntoTotalFunction(
      directPactStates: Seq[(String, SetupProviderState)],
      patternMatchedStates: PartialFunction[String, ProviderStateResult]
  ): SetupProviderState = {
    val l = directPactStates
      .map { case (state, config) =>
        { case s: String if s == state => config(state) }: PartialFunction[String, ProviderStateResult]
      }

    l match {
      case Nil =>
        patternMatchedStates orElse { case _: String => ProviderStateResult() }

      case x :: xs =>
        xs.foldLeft(x)(_ orElse _) orElse patternMatchedStates orElse { case _: String => ProviderStateResult() }

    }
  }
}

package com.itv.scalapact.plugin.shared

import com.itv.scalapact.shared.ColourOuput._
import com.itv.scalapact.shared._
import com.itv.scalapactcore.common.LocalPactFileLoader
import com.itv.scalapactcore.verifier.Verifier._
import com.itv.scalapact.shared.typeclasses.{IPactReader, IScalaPactHttpClient}
import com.itv.scalapact.shared.ProviderStateResult.SetupProviderState

object ScalaPactVerifyCommand {

  def doPactVerify[F[_]](
      scalaPactSettings: ScalaPactSettings,
      providerStates: Seq[(String, SetupProviderState)],
      providerStateMatcher: PartialFunction[String, ProviderStateResult],
      pactBrokerAddress: String,
      projectVersion: String,
      providerName: String,
      consumerNames: Seq[String],
      versionedConsumerNames: Seq[(String, String)]
  )(implicit pactReader: IPactReader, httpClient: IScalaPactHttpClient[F], publisher: IResultPublisher
  ): Unit = {
    PactLogger.message("*************************************".white.bold)
    PactLogger.message("** ScalaPact: Running Verifier     **".white.bold)
    PactLogger.message("*************************************".white.bold)

    val combinedPactStates = combineProviderStatesIntoTotalFunction(providerStates, providerStateMatcher)

    val pactVerifySettings = PactVerifySettings(
      combinedPactStates,
      pactBrokerAddress,
      projectVersion,
      providerName,
      consumerNames.toList,
      versionedConsumerNames = versionedConsumerNames.toList
        .map(t => VersionedConsumer(t._1, t._2))
    )

    val stringToSettingsToPacts = LocalPactFileLoader.loadPactFiles(pactReader)(true)
    val successfullyVerified = verify(stringToSettingsToPacts, pactVerifySettings)(
      pactReader, new SslContextMap(Map()), httpClient, publisher
    )(scalaPactSettings)

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

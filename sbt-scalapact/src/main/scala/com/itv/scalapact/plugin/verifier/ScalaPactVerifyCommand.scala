package com.itv.scalapact.plugin.verifier

import com.itv.scalapact.shared.ColourOuput._
import com.itv.scalapact.shared.{ScalaPactSettings, SslContextMap}
import com.itv.scalapactcore.common.LocalPactFileLoader
import com.itv.scalapactcore.verifier._
import com.itv.scalapactcore.verifier.Verifier._
import com.itv.scalapact.shared.PactLogger
import com.itv.scalapact.shared.typeclasses.{IPactReader, IScalaPactHttpClient}

object ScalaPactVerifyCommand {

  def doPactVerify[F[_]](scalaPactSettings: ScalaPactSettings,
                         providerStates: Seq[(String, String => Boolean)],
                         providerStateMatcher: PartialFunction[String, Boolean],
                         pactBrokerAddress: String,
                         projectVersion: String,
                         providerName: String,
                         consumerNames: Seq[String],
                         versionedConsumerNames: Seq[(String, String)])(implicit pactReader: IPactReader,
                                                                        httpClient: IScalaPactHttpClient[F]): Unit = {

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

    val successfullyVerified =
      verify(LocalPactFileLoader.loadPactFiles(pactReader)(true), pactVerifySettings)(pactReader,
                                                                                      new SslContextMap(Map()),
                                                                                      httpClient)(scalaPactSettings)

    if (successfullyVerified) sys.exit(0) else sys.exit(1)

  }

  def combineProviderStatesIntoTotalFunction(
      directPactStates: Seq[(String, String => Boolean)],
      patternMatchedStates: PartialFunction[String, Boolean]): String => Boolean = {
    val l = directPactStates
      .map { ps =>
        { case s: String if s == ps._1 => ps._2(ps._1) }: PartialFunction[String, Boolean]
      }

    l match {
      case Nil =>
        patternMatchedStates orElse { case _: String => false }

      case x :: xs =>
        xs.foldLeft(x)(_ orElse _) orElse patternMatchedStates orElse { case _: String => false }

    }
  }
}

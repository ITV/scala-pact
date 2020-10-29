package com.itv.scalapactcore.verifier

import com.itv.scalapact.shared.ColourOutput.ColouredString
import com.itv.scalapact.shared.typeclasses.{IPactReader, IResultPublisherBuilder, IScalaPactHttpClient}
import com.itv.scalapact.shared.{PactLogger, PrePactsForVerificationSettings, ScalaPactSettings}

import scala.util.Left

private[verifier] class PrePactsForVerificationVerifier[F[_]](
  brokerClient: PactBrokerClient[F],
  localVerifierClient: IScalaPactHttpClient[F])(
  implicit pactReader: IPactReader,
  publisherBuilder: IResultPublisherBuilder) {

  def verify(pactVerifySettings: PrePactsForVerificationSettings, scalaPactSettings: ScalaPactSettings): Boolean = {

    val pacts = brokerClient.prePactsForVerificationEndpointFetch(pactVerifySettings)

    PactLogger.message(
      s"Verifying against '${scalaPactSettings.giveHost}' on port '${scalaPactSettings.givePort}' with a timeout of ${scalaPactSettings.giveClientTimeout.toSeconds.toString} second(s).".white.bold
    )

    val startTime = System.currentTimeMillis()

    val pactVerifyResults = pacts.map(VerificationSteps.runVerificationAgainst(localVerifierClient, scalaPactSettings, pactVerifySettings.providerStates))

    val endTime      = System.currentTimeMillis()
    val testCount    = pactVerifyResults.flatMap(_.results).length
    val failureCount = pactVerifyResults.flatMap(_.results).count(_.result.isLeft)

    VerificationSteps.writeToJUnit(pactVerifyResults, startTime, endTime, testCount, failureCount)

    pactVerifyResults.foreach { result =>
      PactLogger.message(
        ("Results for pact between " + result.pact.consumer.name + " and " + result.pact.provider.name).white.bold
      )
      result.results.foreach { res =>
        res.result match {
          case Right(_) =>
            PactLogger.message((" - [  OK  ] " + res.context).green)

          case Left(l) =>
            PactLogger.error((" - [FAILED] " + res.context + "\n" + l).red)
        }
      }
    }

    VerificationSteps.logVerificationResults(startTime, endTime, testCount - failureCount, failureCount, 0)

    val _ = VerificationSteps.publishResults(
      scalaPactSettings.publishResultsEnabled,
      pactVerifySettings.pactBrokerClientTimeout,
      pactVerifySettings.sslContextName,
      pactVerifySettings.pactBrokerAuthorization,
      pactVerifyResults
    )

    testCount > 0 && failureCount == 0
  }
}

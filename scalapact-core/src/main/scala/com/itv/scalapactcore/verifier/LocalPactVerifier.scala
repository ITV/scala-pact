package com.itv.scalapactcore.verifier

import com.itv.scalapact.shared.{LocalPactVerifySettings, Pact, ScalaPactSettings}
import com.itv.scalapact.shared.utils.ColourOutput.ColouredString
import com.itv.scalapact.shared.http.IScalaPactHttpClient
import com.itv.scalapact.shared.json.IPactReader
import com.itv.scalapact.shared.utils.PactLogger
import com.itv.scalapactcore.common.LocalPactFileLoader

import scala.util.Left

private[verifier] class LocalPactVerifier(localVerifierClient: IScalaPactHttpClient)(implicit pactReader: IPactReader) {

  def verify(pactVerifySettings: LocalPactVerifySettings, scalaPactSettings: ScalaPactSettings): Boolean = {

    PactLogger.message(
      s"Attempting to use local pact files at: '${scalaPactSettings.localPactFilePath.getOrElse("<path missing>")}'".white.bold
    )

    val pacts = LocalPactFileLoader.loadPactFiles[Pact](allowTmpFiles = true, "pacts")(scalaPactSettings)

    PactLogger.message(
      s"Verifying against '${scalaPactSettings.giveHost}' on port '${scalaPactSettings.givePort}' with a timeout of ${scalaPactSettings.giveClientTimeout.toSeconds.toString} second(s).".white.bold
    )

    val startTime = System.currentTimeMillis()

    val pactVerifyResults = pacts.map(
      VerificationSteps.runVerificationAgainst(
        localVerifierClient,
        scalaPactSettings,
        pactVerifySettings.providerStates
      )
    )

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

    testCount > 0 && failureCount == 0
  }
}

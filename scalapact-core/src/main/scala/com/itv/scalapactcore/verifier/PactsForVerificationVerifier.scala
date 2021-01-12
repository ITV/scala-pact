package com.itv.scalapactcore.verifier

import com.itv.scalapact.shared.utils.ColourOutput.ColouredString
import com.itv.scalapact.shared.Notice.{AfterVerificationNotice, BeforeVerificationNotice, SimpleNotice}
import com.itv.scalapact.shared.http.IScalaPactHttpClient
import com.itv.scalapact.shared.json.IPactReader
import com.itv.scalapact.shared.utils.PactLogger
import com.itv.scalapact.shared._
import com.itv.scalapactcore.common.PactBrokerClient
import com.itv.scalapactcore.verifier.PactsForVerificationVerifier.VerificationResult

import scala.util.Left

private[verifier] class PactsForVerificationVerifier(
    pactBrokerClient: PactBrokerClient,
    localVerifierClient: IScalaPactHttpClient
)(implicit pactReader: IPactReader) {
  def verify(verificationSettings: PactsForVerificationSettings, scalaPactSettings: ScalaPactSettings): Boolean = {

    val pactsAndProperties = pactBrokerClient.fetchPactsFromPactsForVerification(verificationSettings)

    PactLogger.message(
      s"Verifying against '${scalaPactSettings.giveHost}' on port '${scalaPactSettings.givePort}' with a timeout of ${scalaPactSettings.giveClientTimeout.toSeconds.toString} second(s).".white.bold
    )

    val start = System.currentTimeMillis()

    val resultsAndProperties = pactsAndProperties.map { case (pact, properties) =>
      (
        VerificationSteps.runVerificationAgainst(
          localVerifierClient,
          scalaPactSettings,
          verificationSettings.providerStates
        )(pact),
        properties
      )
    }

    val end = System.currentTimeMillis()

    val published = scalaPactSettings.publishResultsEnabled.exists { publishData =>
      pactBrokerClient.publishVerificationResults(
        resultsAndProperties.map(_._1),
        publishData,
        verificationSettings.providerVersionTags,
        verificationSettings.pactBrokerAuthorization,
        verificationSettings.pactBrokerClientTimeout,
        verificationSettings.sslContextName
      )
      true
    }

    val testCount = resultsAndProperties.length

    val formattedResult = formatResults(resultsAndProperties, published)
    val successCount    = formattedResult.collect { case _: VerificationResult.Success.type => true }.length
    val failureCount    = formattedResult.collect { case _: VerificationResult.Failed.type => true }.length
    val pendingCount    = testCount - successCount - failureCount

    VerificationSteps.writeToJUnit(resultsAndProperties.map(_._1), start, end, testCount, failureCount)
    VerificationSteps.logVerificationResults(start, end, successCount, failureCount, pendingCount)

    testCount > 0 && failureCount == 0
  }

  private def formatResults(
      results: List[(PactVerifyResult, VerificationProperties)],
      published: Boolean
  ): List[VerificationResult] =
    results.map { case (result, properties) =>
      PactLogger.message(
        ("--------------------\n" +
          "Results for pact between " + result.pact.consumer.name + " and " + result.pact.provider.name + " - \n").white.bold
      )
      val allInteractionsSucceeded: Boolean = result.results.forall(_.result.isRight)
      val isStillPending: Boolean =
        properties.pending && ((allInteractionsSucceeded && !published) || !allInteractionsSucceeded)
      val beforeVerificationNotices = properties.notices.collect {
        case n: BeforeVerificationNotice => n.text
        case n: SimpleNotice             => n.text
      }
      val afterVerificationNotice = properties.notices.collectFirst {
        case AfterVerificationNotice(text, s, p) if s == allInteractionsSucceeded && p == published => text
      }

      beforeVerificationNotices.foreach { notice =>
        PactLogger.message((" - " + notice).white.bold)
      }

      result.results.foreach {
        case PactVerifyResultInContext(Right(_), context) =>
          if (isStillPending) PactLogger.message((" - [PENDING] " + context).yellow)
          else PactLogger.message((" - [  OK  ] " + context).green)
        case PactVerifyResultInContext(Left(l), context) =>
          if (isStillPending) PactLogger.message((" - [PENDING] " + context + "\n" + l).yellow)
          else PactLogger.error((" - [FAILED] " + context + "\n" + l).red)
      }

      afterVerificationNotice.foreach(text => PactLogger.message((" - " + text).white.bold))

      if (isStillPending) VerificationResult.Pending
      else if (allInteractionsSucceeded) VerificationResult.Success
      else VerificationResult.Failed
    }
}

private[verifier] object PactsForVerificationVerifier {
  sealed trait VerificationResult extends Product with Serializable

  object VerificationResult {
    case object Success extends VerificationResult
    case object Failed  extends VerificationResult
    case object Pending extends VerificationResult
  }
}

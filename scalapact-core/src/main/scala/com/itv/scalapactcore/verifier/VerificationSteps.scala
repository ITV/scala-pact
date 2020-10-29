package com.itv.scalapactcore.verifier

import com.itv.scalapact.shared.ColourOutput.ColouredString
import com.itv.scalapact.shared.ProviderStateResult.SetupProviderState
import com.itv.scalapact.shared.typeclasses.{BrokerPublishData, IPactReader, IResultPublisherBuilder, IScalaPactHttpClient}
import com.itv.scalapact.shared.{InteractionRequest, InteractionResponse, Pact, PactBrokerAuthorization, PactLogger, PactVerifyResult, PactVerifyResultInContext, ProviderStateResult, ScalaPactSettings}
import com.itv.scalapactcore.common.matching.InteractionMatchers.matchResponse

import scala.concurrent.duration._
import scala.util.Left

object VerificationSteps {
  def runVerificationAgainst[F[_]](
    client: IScalaPactHttpClient[F],
    arguments: ScalaPactSettings,
    providerStates: SetupProviderState)(pact: Pact)(implicit pactReader: IPactReader): PactVerifyResult = {
    val results = pact.interactions.map { interaction =>
      val maybeProviderState = interaction.providerState.map(p => ProviderState(p, providerStates))
      val result = runInteractionRequest(client, arguments, maybeProviderState, interaction.request).flatMap(matchResponse(arguments.giveStrictMode, List(interaction))(pactReader))

      PactVerifyResultInContext(result, interaction.description)
    }
    PactVerifyResult(pact, results)
  }

  def writeToJUnit(results: List[PactVerifyResult], start: Long, end: Long, testCount: Int, failureCount: Int): Unit = results.foreach { result =>
    JUnitXmlBuilder.xml(
      name = result.pact.consumer.name + " - " + result.pact.provider.name,
      tests = testCount,
      failures = failureCount,
      time = end.toDouble - start.toDouble / 1000,
      testCases = result.results.map { res =>
        res.result match {
          case Right(_) =>
            JUnitXmlBuilder.testCasePass(res.context)

          case Left(l) =>
            JUnitXmlBuilder.testCaseFail("Failure: " + res.context, l)
        }
      }
    )
  }

  def publishResults(
                      publishResultsEnabled: Option[BrokerPublishData],
                      clientTimeout: Option[Duration],
                      sslContextName: Option[String],
                      auth: Option[PactBrokerAuthorization],
                      results: List[PactVerifyResult])(implicit publisherBuilder: IResultPublisherBuilder): Boolean =
    publishResultsEnabled.exists { publishData =>
      publisherBuilder.buildWithDefaults(clientTimeout, sslContextName)
        .publishResults(results, publishData, auth)
      true
    }

  def logVerificationResults(startTime: Long, endTime: Long, successCount: Int, failureCount: Int, pendingCount: Int): Unit = {
    val testCount = {successCount + failureCount + pendingCount}
    val scalaPactLogPrefix = "[scala-pact] ".white
    PactLogger.message(scalaPactLogPrefix + s"Run completed in: ${endTime - startTime} ms".yellow)
    PactLogger.message(scalaPactLogPrefix + s"Total number of test run: $testCount".yellow)
    PactLogger.message(
      scalaPactLogPrefix + s"Tests: succeeded $successCount, failed $failureCount, pending $pendingCount".yellow
    )

    if (testCount == 0)
      PactLogger.message(scalaPactLogPrefix + "No Pact verification tests run.".red)
    else if (failureCount == 0)
      PactLogger.message(scalaPactLogPrefix + "All Pact verify tests passed or pending.".green)
    else
      PactLogger.message(scalaPactLogPrefix + s"$failureCount Pact verify tests failed.".red)
  }

  private def runInteractionRequest[F[_]](
    client: IScalaPactHttpClient[F],
    arguments: ScalaPactSettings,
    maybeProviderState: Option[ProviderState],
    interactionRequest: InteractionRequest): Either[String, InteractionResponse] = {
      val baseUrl       = s"${arguments.giveProtocol}://" + arguments.giveHost + ":" + arguments.givePort.toString
      val finalRequest = try {
        maybeProviderState match {
          case Some(ps) =>
            PactLogger.message("--------------------".yellow.bold)
            PactLogger.message(s"Attempting to run provider state: ${ps.key}".yellow.bold)

            val ProviderStateResult(success, modifyRequest) = ps.f(ps.key)

            if (success)
              PactLogger.message(s"Provider state ran successfully".yellow.bold)
            else
              PactLogger.error(s"Provider state run failed".red.bold)

            PactLogger.message("--------------------".yellow.bold)

            if (!success) {
              throw ProviderStateFailure(ps.key)
            }

            modifyRequest(interactionRequest)
          case None =>
            // No provider state run needed
            interactionRequest
        }
      } catch {
        case t: Throwable =>
          if (maybeProviderState.isDefined) {
            PactLogger.error(
              s"Error executing unknown provider state function with key: ${maybeProviderState.map(_.key).getOrElse("<missing key>")}".red
            )
          } else {
            PactLogger.error("Error executing unknown provider state function!".red)
          }
          throw t
      }

      try {
        InteractionRequest.unapply(finalRequest) match {
          case Some((Some(_), Some(_), _, _, _, _)) =>
            client.doInteractionRequestSync(baseUrl,
              finalRequest.withoutSslContextHeader) match {
              case Left(e) =>
                PactLogger.error(s"Error in response: ${e.getMessage}".red)
                Left(e.getMessage)
              case Right(r) =>
                Right(r)
            }

          case _ => Left("Invalid request was missing either method or path: " + interactionRequest.renderAsString)

        }
      } catch {
        case e: Throwable =>
          Left(e.getMessage)
      }
    }

}

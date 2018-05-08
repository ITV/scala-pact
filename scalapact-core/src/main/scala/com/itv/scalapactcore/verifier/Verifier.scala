package com.itv.scalapactcore.verifier

import com.itv.scalapact.shared._
import com.itv.scalapactcore.common.matching.InteractionMatchers._
import com.itv.scalapact.shared.ColourOuput._
import com.itv.scalapactcore.common._

import scala.util.Left
import com.itv.scalapact.shared.PactLogger
import com.itv.scalapact.shared.typeclasses.{IPactReader, IScalaPactHttpClient}

object Verifier {

  def verify[F[_]](
      loadPactFiles: String => ScalaPactSettings => ConfigAndPacts,
      pactVerifySettings: PactVerifySettings
  )(implicit pactReader: IPactReader,
    sslContextMap: SslContextMap,
    httpClient: IScalaPactHttpClient[F]): ScalaPactSettings => Boolean = arguments => {

    val scalaPactLogPrefix = "[scala-pact] ".white

    val pacts: List[Pact] = if (arguments.localPactFilePath.isDefined) {
      PactLogger.message(
        s"Attempting to use local pact files at: '${arguments.localPactFilePath.getOrElse("<path missing>")}'".white.bold
      )
      loadPactFiles("pacts")(arguments).pacts
    } else {

      val versionConsumers =
        pactVerifySettings.consumerNames.map(c => VersionedConsumer(c, "/latest")) ++
          pactVerifySettings.versionedConsumerNames.map(vc => vc.copy(version = "/version/" + vc.version))

      val latestPacts: List[Pact] = versionConsumers
        .map { consumer =>
          ValidatedDetails.buildFrom(consumer.name,
                                     pactVerifySettings.providerName,
                                     pactVerifySettings.pactBrokerAddress,
                                     consumer.version) match {
            case Left(l) =>
              PactLogger.error(l.red)
              None

            case Right(v) =>
              fetchAndReadPact(
                v.validatedAddress.address + "/pacts/provider/" + v.providerName + "/consumer/" + v.consumerName + v.consumerVersion
              )
          }
        }
        .collect {
          case Some(s) => s
        }

      latestPacts
    }

    PactLogger.message(
      s"Verifying against '${arguments.giveHost}' on port '${arguments.givePort}' with a timeout of ${arguments.clientTimeout
        .map(_.toSeconds.toString)
        .getOrElse("<unspecified>")} second(s).".white.bold
    )

    val startTime = System.currentTimeMillis().toDouble

    val pactVerifyResults = pacts.map { pact =>
      PactVerifyResult(
        pact = pact,
        results = pact.interactions.map { interaction =>
          val maybeProviderState =
            interaction.providerState
              .map(p => ProviderState(p, pactVerifySettings.providerStates))

          val result =
            (doRequest(arguments, maybeProviderState) andThen attemptMatch(arguments.giveStrictMode,
                                                                           List(interaction)))(interaction.request)

          PactVerifyResultInContext(result, interaction.description)
        }
      )
    }

    val endTime      = System.currentTimeMillis().toDouble
    val testCount    = pactVerifyResults.flatMap(_.results).length
    val failureCount = pactVerifyResults.flatMap(_.results).count(_.result.isLeft)

    pactVerifyResults.foreach { result =>
      val content = JUnitXmlBuilder.xml(
        name = result.pact.consumer.name + " - " + result.pact.provider.name,
        tests = testCount,
        failures = failureCount,
        time = endTime - startTime / 1000,
        testCases = result.results.map { res =>
          res.result match {
            case Right(_) =>
              JUnitXmlBuilder.testCasePass(res.context)

            case Left(l) =>
              JUnitXmlBuilder.testCaseFail("Failure: " + res.context, l)
          }
        }
      )
      JUnitWriter.writePactVerifyResults(result.pact.consumer.name)(result.pact.provider.name)(content.toString)
    }

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

    PactLogger.message(scalaPactLogPrefix + s"Run completed in: ${(endTime - startTime).toInt} ms".yellow)
    PactLogger.message(scalaPactLogPrefix + s"Total number of test run: $testCount".yellow)
    PactLogger.message(scalaPactLogPrefix + s"Tests: succeeded ${testCount - failureCount}, failed $failureCount".yellow)
    if (failureCount == 0) PactLogger.message(scalaPactLogPrefix + "All tests passed.".green)
    else PactLogger.message(scalaPactLogPrefix + s"$failureCount tests failed.".red)
    failureCount == 0
  }

  private def attemptMatch(strictMatching: Boolean, interactions: List[Interaction])(
      implicit pactReader: IPactReader
  ): Either[String, InteractionResponse] => Either[String, Interaction] = {
    case Right(i) =>
      matchResponse(strictMatching, interactions)(pactReader)(i)

    case Left(s) =>
      Left(s)
  }

  private def doRequest[F[_]](arguments: ScalaPactSettings, maybeProviderState: Option[ProviderState])(
      implicit sslContextMap: SslContextMap,
      httpClient: IScalaPactHttpClient[F]
  ): InteractionRequest => Either[String, InteractionResponse] =
    interactionRequest => {
      val baseUrl       = s"${arguments.giveProtocol}://" + arguments.giveHost + ":" + arguments.givePort.toString
      val clientTimeout = arguments.giveClientTimeout

      try {

        maybeProviderState match {
          case Some(ps) =>
            PactLogger.message("--------------------".yellow.bold)
            PactLogger.message(s"Attempting to run provider state: ${ps.key}".yellow.bold)

            val success = ps.f(ps.key)

            if (success)
              PactLogger.message(s"Provider state ran successfully".yellow.bold)
            else
              PactLogger.error(s"Provider state run failed".red.bold)

            PactLogger.message("--------------------".yellow.bold)

            if (!success) {
              throw ProviderStateFailure(ps.key)
            }

          case None =>
          // No provider state run needed
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
        InteractionRequest.unapply(interactionRequest) match {
          case Some((Some(_), Some(_), _, _, _, _)) =>
            httpClient.doInteractionRequestSync(baseUrl,
                                                interactionRequest.withoutSslContextHeader,
                                                clientTimeout,
                                                interactionRequest.sslContextName) match {
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

  private def fetchAndReadPact[F[_]](address: String)(implicit pactReader: IPactReader,
                                                      sslContextMap: SslContextMap,
                                                      httpClient: IScalaPactHttpClient[F]): Option[Pact] = {

    PactLogger.message(s"Attempting to fetch pact from pact broker at: $address".white.bold)

    httpClient
      .doRequestSync(
        SimpleRequest(address, "", HttpMethod.GET, Map("Accept" -> "application/json"), None, sslContextName = None)
      ) match {
      case Left(e) =>
        PactLogger.error(s"Error: ${e.getMessage}".yellow)
        None

      case Right(response) if response.is2xx =>
        response.body.map(pactReader.jsonStringToPact).flatMap {
          case Right(p) =>
            Option(p)

          case Left(msg) =>
            PactLogger.error("Could not convert good response to Pact:\n" + response.body.getOrElse(""))
            PactLogger.error(s"Error: $msg".yellow)
            None
        }

      case Right(_) =>
        PactLogger.error(s"Failed to load consumer pact from: $address".red)
        None
    }

  }

}

case class PactVerifyResult(pact: Pact, results: List[PactVerifyResultInContext])

case class PactVerifyResultInContext(result: Either[String, Interaction], context: String)

case class ProviderStateFailure(key: String) extends Exception()

case class ProviderState(key: String, f: String => Boolean)

case class VersionedConsumer(name: String, version: String)

case class PactVerifySettings(providerStates: (String => Boolean),
                              pactBrokerAddress: String,
                              projectVersion: String,
                              providerName: String,
                              consumerNames: List[String],
                              versionedConsumerNames: List[VersionedConsumer])

case class ValidatedDetails(validatedAddress: ValidPactBrokerAddress,
                            providerName: String,
                            consumerName: String,
                            consumerVersion: String)

object ValidatedDetails {

  def buildFrom(consumerName: String,
                providerName: String,
                pactBrokerAddress: String,
                consumerVersion: String): Either[String, ValidatedDetails] = {

    val values = (Helpers.urlEncode(consumerName),
                  Helpers.urlEncode(providerName),
                  PactBrokerAddressValidation.checkPactBrokerAddress(pactBrokerAddress))

    values match {
      case (Right(consumer), Right(provider), Right(validatedAddress)) =>
        Right(ValidatedDetails(validatedAddress, provider, consumer, consumerVersion))

      case (Left(e), _, _) =>
        Left(e)

      case (_, Left(e), _) =>
        Left(e)

      case (_, _, Left(e)) =>
        Left(e)
    }
  }

}

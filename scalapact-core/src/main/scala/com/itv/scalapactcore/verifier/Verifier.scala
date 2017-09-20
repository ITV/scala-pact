package com.itv.scalapactcore.verifier

import com.itv.scalapactcore.common.matching.InteractionMatchers._
import com.itv.scalapactcore.common.ColourOuput._
import com.itv.scalapactcore.common._
import com.itv.scalapactcore.common.pact._

import scala.util.Left

object Verifier {

  private final case class ValidatedDetails(validatedAddress: ValidPactBrokerAddress, providerName: String, consumerName: String, consumerVersion: String)

  def verify(pactVerifySettings: PactVerifySettings): Arguments => Boolean = arguments => {

    val pacts: List[Pact] = if(arguments.localPactPath.isDefined) {
      println(s"Attempting to use local pact files at: '${arguments.localPactPath.getOrElse("<path missing>")}'".white.bold)
      LocalPactFileLoader.loadPactFiles("pacts")(arguments).pacts
    } else {

      val versionConsumers =
        pactVerifySettings.consumerNames.map(c => VersionedConsumer(c, "/latest")) ++
          pactVerifySettings.versionedConsumerNames.map(vc => vc.copy(version = "/version/" + vc.version))

      val latestPacts : List[Pact] = versionConsumers.map { consumer =>

        val details: Either[String, ValidatedDetails] = {
          for {
            c  <- Helpers.urlEncode(consumer.name)
            p  <- Helpers.urlEncode(pactVerifySettings.providerName)
            pb <- PactBrokerAddressValidation.checkPactBrokerAddress(pactVerifySettings.pactBrokerAddress)
          } yield ValidatedDetails(pb, p, c, consumer.version)
        }

        details match {
          case Left(l) =>
            println(l.red)
            None

          case Right(v) =>
            fetchAndReadPact(v.validatedAddress.address + "/pacts/provider/" + v.providerName + "/consumer/" + v.consumerName + v.consumerVersion)
        }
      }.collect {
        case Some(s) => s
      }

      latestPacts
    }

    println(s"Verifying against '${arguments.giveHost}' on port '${arguments.givePort}' with a timeout of ${arguments.clientTimeout.getOrElse(1)} second(s).".white.bold)

    val startTime = System.currentTimeMillis().toDouble

    val pactVerifyResults = pacts.map { pact =>
      PactVerifyResult(
        pact = pact,
        results = pact.interactions.map { interaction =>

          val maybeProviderState =
            interaction
              .providerState
              .map(p => ProviderState(p, PartialFunction(pactVerifySettings.providerStates)))

          (doRequest(arguments, maybeProviderState) andThen attemptMatch(arguments.giveStrictMode, List(interaction)))(interaction.request)
        }
      )
    }

    val endTime = System.currentTimeMillis().toDouble
    val testCount = pactVerifyResults.flatMap(_.results).length
    val failureCount = pactVerifyResults.flatMap(_.results).count(_.isLeft)

    pactVerifyResults.foreach { result =>
      val content = JUnitXmlBuilder.xml(
        name = result.pact.consumer.name + " - " + result.pact.provider.name,
        tests = testCount,
        failures = failureCount,
        time = endTime - startTime / 1000,
        testCases = result.results.collect {
          case Right(r) => JUnitXmlBuilder.testCasePass(r.description)
          case Left(l) => JUnitXmlBuilder.testCaseFail("Failure", l)
        }
      )
      JUnitWriter.writePactVerifyResults(result.pact.consumer.name)(result.pact.provider.name)(content.toString)
    }

    pactVerifyResults.foreach { result =>
      println(("Results for pact between " + result.pact.consumer.name + " and " + result.pact.provider.name).white.bold)
      result.results.foreach {
        case Right(r) => println((" - [  OK  ] " + r.description).green)
        case Left(l) => println((" - [FAILED] " + l).red)
      }
    }

    val foundErrors = pactVerifyResults.flatMap(result => result.results).exists(_.isLeft)

    !foundErrors
  }

  private def attemptMatch(strictMatching: Boolean, interactions: List[Interaction]): Either[String, InteractionResponse] => Either[String, Interaction] = requestResult =>
    requestResult.flatMap(matchResponse(strictMatching, interactions))

  private def doRequest(arguments: Arguments, maybeProviderState: Option[ProviderState]): InteractionRequest => Either[String, InteractionResponse] = interactionRequest => {
    val baseUrl = s"${arguments.giveProtocol}://" + arguments.giveHost + ":" + arguments.givePort
    val clientTimeout = arguments.giveClientTimeoutInSeconds

    try {

      maybeProviderState match {
        case Some(ps) =>
          println("--------------------".yellow.bold)
          println(s"Attempting to run provider state: ${ps.key}".yellow.bold)

          val success = ps.f(ps.key)

          if(success)
            println(s"Provider state ran successfully".yellow.bold)
          else
            println(s"Provider state run failed".red.bold)

          println("--------------------".yellow.bold)

          if(!success) {
            throw new ProviderStateFailure(ps.key)
          }

        case None =>
          // No provider state run needed
      }

    } catch {
      case t: Throwable =>
        if(maybeProviderState.isDefined) {
          println(s"Error executing unknown provider state function with key: ${maybeProviderState.map(_.key).getOrElse("<missing key>")}".red)
        } else {
          println("Error executing unknown provider state function!".red)
        }
        throw t
    }

    try {
      InteractionRequest.unapply(interactionRequest) match {
        case Some((Some(_), Some(_), _, _, _, _)) =>

          ScalaPactHttp.doInteractionRequest(baseUrl, interactionRequest, clientTimeout)
            .unsafePerformSyncAttempt
            .leftMap { t =>
              println(s"Error in response: ${t.getMessage}".red)
              t.getMessage
            }
            .toEither

        case _ => Left("Invalid request was missing either method or path: " + interactionRequest)

      }
    } catch {
      case e: Throwable =>
        Left(e.getMessage)
    }

  }

  private def fetchAndReadPact(address: String): Option[Pact] = {
    println(s"Attempting to fetch pact from pact broker at: $address".white.bold)

    ScalaPactHttp.doRequest(ScalaPactHttp.GET, address, "", Map("Accept" -> "application/json"), None).map {
      case r: SimpleResponse if r.is2xx =>
        val pact = r.body.map(PactReader.jsonStringToPact).flatMap {
          case Right(p) => Option(p)
          case Left(msg) =>
            println(s"Error: $msg".yellow)
            None
        }

        if(pact.isEmpty) {
          println("Could not convert good response to Pact:\n" + r.body)
          pact
        } else pact

      case _ =>
        println(s"Failed to load consumer pact from: $address".red)
        None
    }.unsafePerformSyncAttempt.toEither match {
      case Right(p) => p
      case Left(e) =>
        println(s"Error: ${e.getMessage}".yellow)
        None
    }

  }

}

case class PactVerifyResult(pact: Pact, results: List[Either[String, Interaction]])

class ProviderStateFailure(key: String) extends Exception()

case class ProviderState(key: String, f: String => Boolean)
case class VersionedConsumer(name: String, version: String)
case class PactVerifySettings(providerStates: (String => Boolean), pactBrokerAddress: String, projectVersion: String, providerName: String, consumerNames: List[String], versionedConsumerNames: List[VersionedConsumer])

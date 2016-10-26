package com.itv.scalapactcore.verifier

import com.itv.scalapactcore._
import com.itv.scalapactcore.common.matching.InteractionMatchers._
import com.itv.scalapactcore.common.ColourOuput._
import com.itv.scalapactcore.common._
import argonaut._
import Argonaut._
import PactImplicits._

import scala.util.Left

object Verifier {

  private final case class ValidatedDetails(validatedAddress: String, providerName: String, consumerName: String, consumerVersion: String)

  lazy val verify: PactVerifySettings => Arguments => Boolean = pactVerifySettings => arguments => {

    val pacts: List[Pact] = if(arguments.localPactPath.isDefined) {
      println(s"Attempting to use local pact files at: '${arguments.localPactPath.get}'".white.bold)
      LocalPactFileLoader.loadPactFiles("pacts")(arguments).pacts
    } else {

      val versionConsumers =
        pactVerifySettings.consumerNames.map(c => VersionedConsumer(c, "/latest")) ++
          pactVerifySettings.versionedConsumerNames.map(vc => vc.copy(version = "/version/" + vc.version))

      val latestPacts : List[Pact] = versionConsumers.map { consumer =>

        // I'm missing applicative builder now I can tell you...
        val details: Either[String, ValidatedDetails] =
          Helpers.urlEncode(consumer.name) match {
            case Right(consumerName) =>
              Helpers.urlEncode(pactVerifySettings.providerName) match {
                case Right(providerName) =>
                  PactBrokerAddressValidation.checkPactBrokerAddress(pactVerifySettings.pactBrokerAddress) match {
                    case Right(validatedAddress) =>
                      Right(ValidatedDetails(validatedAddress, providerName, consumerName, consumer.version))
                    case Left(l) => Left(l)
                  }
                case Left(l) => Left(l)
              }
            case Left(l) => Left(l)
          }

        details match {
          case Left(l) =>
            println(l.red)
            None

          case Right(v) =>
            fetchAndReadPact(v.validatedAddress + "/pacts/provider/" + v.providerName + "/consumer/" + v.consumerName + v.consumerVersion)
        }
      }.collect {
        case Some(s) => s
      }

      latestPacts
    }

    println(s"Verifying against '${arguments.giveHost}', port '${arguments.givePort}'".white.bold)

    val startTime = System.currentTimeMillis().toDouble

    val errorMessage: Interaction => String => String = interaction => message =>
      s"""No matching response for: '${interaction.description}'
          |Expected:
          |${interaction.response.asJson.pretty(PrettyParams.spaces2.copy(dropNullKeys = true))}
          |Actual:
          |$message
          |---
       """.stripMargin

    val pactVerifyResults = pacts.map { pact =>
      PactVerifyResult(
        pact = pact,
        results = pact.interactions.map { interaction =>

          val maybeProviderState = interaction.providerState.flatMap(p => pactVerifySettings.providerStates.find(j => j.key == p))

          val matchResult = (doRequest(arguments)(maybeProviderState) andThen attemptMatch(arguments.giveStrictMode)(List(interaction)))(interaction.request)

          matchResult match {
            case r @ Right(_) => r
            case Left(m) => Left(errorMessage(interaction)(m))
          }
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

  private lazy val attemptMatch: Boolean => List[Interaction] => Either[String, InteractionResponse] => Either[String, Interaction] = strictMatching => interactions => requestResult =>
    requestResult match {
      case Right(r) => matchResponse(strictMatching)(interactions)(r)
      case Left(s) => Left(s)
    }

  private lazy val doRequest: Arguments => Option[ProviderState] => InteractionRequest => Either[String, InteractionResponse] = arguments => maybeProviderState => interactionRequest => {
    val baseUrl = s"${arguments.giveProtocol}://" + arguments.giveHost + ":" + arguments.givePort

    try {
      if(maybeProviderState.isDefined) {
        println("--------------------".yellow.bold)
        println(s"Attempting to run provider state: ${maybeProviderState.get.key}".yellow.bold)

        val success = maybeProviderState.get.f(maybeProviderState.get.key)


        if(success) println(s"Provider state ran successfully".yellow.bold)
        else println(s"Provider state run failed".red.bold)
        println("--------------------".yellow.bold)

        if(!success) throw new ProviderStateFailure(maybeProviderState.get.key)
      }
    } catch {
      case e: Throwable =>
        if(maybeProviderState.isDefined) {
          println(s"Error executing unknown provider state function with key: ${maybeProviderState.get.key}".red)
        } else {
          println("Error executing unknown provider state function!".red)
        }
    }

    try {
      InteractionRequest.unapply(interactionRequest) match {
        case Some((Some(_), Some(_), _, _, _, _)) =>

          ScalaPactHttp.doInteractionRequest(baseUrl, interactionRequest)
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
        val pact = r.body.map(ScalaPactReader.jsonStringToPact).flatMap {
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
case class PactVerifySettings(providerStates: List[ProviderState], pactBrokerAddress: String, projectVersion: String, providerName: String, consumerNames: List[String], versionedConsumerNames: List[VersionedConsumer])

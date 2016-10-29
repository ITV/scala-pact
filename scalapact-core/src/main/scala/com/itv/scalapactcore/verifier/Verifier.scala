package com.itv.scalapactcore.verifier

import com.itv.scalapactcore._
import com.itv.scalapactcore.common.matching.InteractionMatchers._
import com.itv.scalapactcore.common.ColourOuput._
import com.itv.scalapactcore.common._
import argonaut._
import Argonaut._
import PactImplicits._

import RightBiasEither._

import scala.util.Left

object Verifier {

  private final case class ValidatedDetails(validatedAddress: String, providerName: String, consumerName: String, consumerVersion: String)

  lazy val verify: PactVerifySettings => Arguments => Boolean = pactVerifySettings => arguments => {

    val pacts: List[Pact] = if(arguments.localPactPath.isDefined) {
      println(s"Attempting to use local pact files at: '${arguments.localPactPath.getOrElse("<path missing>")}'".white.bold)
      LocalPactFileLoader.loadPactFiles("pacts")(arguments).pacts
    } else {

      val versionConsumers =
        pactVerifySettings.consumerNames.map(c => VersionedConsumer(c, "/latest")) ++
          pactVerifySettings.versionedConsumerNames.map(vc => vc.copy(version = "/version/" + vc.version))

      val latestPacts : List[Pact] = versionConsumers.map { consumer =>

        // I'm missing applicative builder now I can tell you...
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

          matchResult.leftMap(errorMessage(interaction))
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
    requestResult.flatMap(matchResponse(strictMatching)(interactions))

  private lazy val doRequest: Arguments => Option[ProviderState] => InteractionRequest => Either[String, InteractionResponse] = arguments => maybeProviderState => interactionRequest => {
    val baseUrl = s"${arguments.giveProtocol}://" + arguments.giveHost + ":" + arguments.givePort

    // Started wart removing...
//    val providerStateRun = maybeProviderState.map { ps =>
//      val key = ps.key
//
//      println(s"Attempting to run provider state: $key".yellow.bold)
//
//      val success = ps.f(key)
//
//      if(success) {
//        println(s"Provider state ran successfully".yellow.bold)
//        Right(success)
//      } else {
//        println(s"Provider state run failed".red.bold)
//        println("--------------------".yellow.bold)
//        println(s"Error executing unknown provider state function with key: $key".red)
//        Left(s"Error executing unknown provider state function with key: $key")
//      }
//    }

    try {

      if(maybeProviderState.isDefined) {
        println("--------------------".yellow.bold)
        val key = maybeProviderState.map(_.key).getOrElse("<missing key>")

        println(s"Attempting to run provider state: $key".yellow.bold)

        val success = maybeProviderState.map(_.f(key)).getOrElse(true)

        if(success) println(s"Provider state ran successfully".yellow.bold)
        else println(s"Provider state run failed".red.bold)
        println("--------------------".yellow.bold)

        if(!success) throw new ProviderStateFailure(key)
      }
    } catch {
      case _: Throwable =>
        if(maybeProviderState.isDefined) {
          println(s"Error executing unknown provider state function with key: ${maybeProviderState.map(_.key).getOrElse("<missing key>")}".red)
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

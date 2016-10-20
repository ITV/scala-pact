package com.itv.scalapact.plugin.verifier

import com.itv.scalapactcore._
import com.itv.scalapactcore.common.matching.InteractionMatchers._
import com.itv.scalapactcore.common.ColourOuput._
import com.itv.scalapactcore.common.{Arguments, Helpers, LocalPactFileLoader, PactBrokerAddressValidation}

import scalaj.http.{Http, HttpResponse}
import scalaz.Scalaz._
import scalaz._

import argonaut._
import Argonaut._

import PactImplicits._

object Verifier {

  lazy val verify: PactVerifySettings => Arguments => Boolean = pactVerifySettings => arguments => {

    val pacts: List[Pact] = if(arguments.localPactPath.isDefined) {
      println(s"Attempting to use local pact files at: '${arguments.localPactPath.get}'".white.bold)
      LocalPactFileLoader.loadPactFiles("pacts")(arguments).pacts
    } else {

      val versionConsumers =
        pactVerifySettings.consumerNames.map(c => VersionedConsumer(c, "/latest")) ++
          pactVerifySettings.versionedConsumerNames.map(vc => vc.copy(version = "/version/" + vc.version))

      val latestPacts : List[Pact] = versionConsumers.map { consumer =>
        val details = for {
          consumerName <- Helpers.urlEncode(consumer.name)
          validatedAddress <- PactBrokerAddressValidation.checkPactBrokerAddress(pactVerifySettings.pactBrokerAddress)
          providerName <- Helpers.urlEncode(pactVerifySettings.providerName)
        } yield (validatedAddress, providerName, consumerName, consumer.version)

        details match {
          case -\/(l) =>
            println(l.red)
            None

          case \/-((b, p, c, v)) =>
            fetchAndReadPact(b + "/pacts/provider/" + p + "/consumer/" + c + v)
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


          matchResult.leftMap(m => errorMessage(interaction)(m))
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
          case \/-(r) => JUnitXmlBuilder.testCasePass(r.description)
          case -\/(l) => JUnitXmlBuilder.testCaseFail("Failure", l)
        }
      )
      JUnitWriter.writePactVerifyResults(result.pact.consumer.name)(result.pact.provider.name)(content.toString)
    }

    pactVerifyResults.foreach { result =>
      println(("Results for pact between " + result.pact.consumer + " and " + result.pact.provider).white.bold)
      result.results.foreach {
        case \/-(r) => println((" - [  OK  ] " + r.description).green)
        case -\/(l) => println((" - [FAILED] " + l).red)
      }
    }

    val foundErrors = pactVerifyResults.flatMap(result => result.results).exists(_.isLeft)

    !foundErrors
  }

  private lazy val attemptMatch: Boolean => List[Interaction] => \/[String, InteractionResponse] => \/[String, Interaction] = strictMatching => interactions => requestResult =>
    requestResult.flatMap(matchResponse(strictMatching)(interactions))

  private lazy val doRequest: Arguments => Option[ProviderState] => InteractionRequest => \/[String, InteractionResponse] = arguments => maybeProviderState => interactionRequest => {
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
        case Some((Some(method), Some(path), params, _, _, _)) =>

          httpResponseToInteractionResponse {
            //TODO: Rewrite this stuff to be less iffy...
            val basicRequest = Http(baseUrl + path + params.map(s => "?" + s).getOrElse(""))

            val withData =
              if (interactionRequest.body.isDefined && interactionRequest.body.exists(!_.isEmpty)) basicRequest.postData(interactionRequest.body.get)
              else basicRequest

            val withHeaders =
              if (interactionRequest.headers.isDefined) withData.headers(interactionRequest.headers.getOrElse(Map.empty[String, String]))
              else withData

            val withMethod =
              withHeaders.method(method)

            withMethod.asString
          }.right

        case _ => ("Invalid request was missing either method or path: " + interactionRequest).left

      }
    } catch {
      case e: Throwable =>
        e.getMessage.left
    }
  }

  def fetchAndReadPact(address: String): Option[Pact] = {
    println(s"Attempting to fetch pact from pact broker at: $address".white.bold)

    Http(address).header("Accept", "application/json").asString match {
      case r: HttpResponse[String] if r.is2xx =>
        val pact = ScalaPactReader.jsonStringToPact(r.body).toOption

        if(pact.isEmpty) {
          println("Could not convert good response to Pact:\n" + r.body)
          pact
        } else pact

      case _ =>
        println(s"Failed to load consumer pact from: $address".red)
        None
    }
  }

  private lazy val httpResponseToInteractionResponse: HttpResponse[String] => InteractionResponse = response =>
    InteractionResponse(
      status = Option(response.code),
      headers = if(response.headers.isEmpty) None else Option(response.headers.map(p => p._1 -> p._2.mkString)),
      body = if(response.body.isEmpty) None else Option(response.body),
      matchingRules = None
    )

}

case class PactVerifyResult(pact: Pact, results: List[\/[String, Interaction]])

class ProviderStateFailure(key: String) extends Exception()

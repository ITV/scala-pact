package com.itv.scalapact.plugin.verifier

import java.net.URLEncoder
import java.nio.charset.StandardCharsets

import com.itv.scalapact.plugin.common.InteractionMatchers._
import com.itv.scalapact.plugin.common.Rainbow._
import com.itv.scalapact.plugin.common.{Arguments, LocalPactFileLoader, PactBrokerAddressValidation}
import com.itv.scalapactcore._

import scalaj.http.{Http, HttpResponse}
import scalaz.Scalaz._
import scalaz._

object Verifier {

  lazy val verify: PactVerifySettings => Arguments => Unit = pactVerifySettings => arguments => {

    val urlEncode: String => String = str => URLEncoder.encode(str, StandardCharsets.UTF_8.toString)

    val pacts: List[Pact] = if(arguments.localPactPath.isDefined) {
      println(s"Attempting to use local pact files at: '${arguments.localPactPath.get}'".white.bold)
      LocalPactFileLoader.loadPactFiles("pacts")(arguments).pacts
    } else {
      println(s"Attempting to fetch pact from pact broker at".white.bold)
      PactBrokerAddressValidation.checkPactBrokerAddress(pactVerifySettings.pactBrokerAddress) match {
        case -\/(l) =>
          println(l.red)
          Nil

        case \/-(r) =>
          pactVerifySettings.consumerNames.map { consumer =>

            val address = pactVerifySettings.pactBrokerAddress + "/pacts/provider/" + urlEncode(pactVerifySettings.providerName) + "/consumer/" + urlEncode(consumer) + "/latest"

            Http(address).asString match {
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

          }.collect {
            case Some(s) => s
          }
      }
    }

    println(s"Verifying against '${arguments.giveHost}', port '${arguments.givePort}'".white.bold)

    val startTime = System.currentTimeMillis().toDouble

    val pactVerifyResults = pacts.map { pact =>
      PactVerifyResult(
        pact = pact,
        results = pact.interactions.map { interaction =>

          val maybeProviderState = interaction.providerState.flatMap(p => pactVerifySettings.providerStates.find(j => j.key == p))

          (doRequest(arguments)(maybeProviderState) andThen attemptMatch(List(interaction)))(interaction.request)
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

    ()
  }

  private lazy val attemptMatch: List[Interaction] => \/[String, InteractionResponse] => \/[String, Interaction] = interactions => requestResult =>
    requestResult.flatMap(matchResponse(interactions))

  private lazy val doRequest: Arguments => Option[ProviderState] => InteractionRequest => \/[String, InteractionResponse] = arguments => maybeProviderState => interactionRequest => {
    val baseUrl = "http://" + arguments.giveHost + ":" + arguments.givePort

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
        case Some((Some(method), Some(path), params, _, _)) =>

          httpResponseToInteractionResponse {
            val basicRequest = Http(baseUrl + path + params.map(s => "?" + s).getOrElse(""))

            val withData =
              if (interactionRequest.body.isDefined) basicRequest.postData(interactionRequest.body.get)
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

  private lazy val httpResponseToInteractionResponse: HttpResponse[String] => InteractionResponse = response =>
    InteractionResponse(
      status = Option(response.code),
      headers = if(response.headers.isEmpty) None else Option(response.headers.map(p => p._1 -> p._2.mkString)),
      body = if(response.body.isEmpty) None else Option(response.body)
    )

}

case class PactVerifyResult(pact: Pact, results: List[\/[String, Interaction]])

class ProviderStateFailure(key: String) extends Exception()
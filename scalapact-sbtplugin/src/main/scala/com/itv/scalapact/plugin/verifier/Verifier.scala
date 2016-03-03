package com.itv.scalapact.plugin.verifier

import com.itv.scalapact.plugin.common.{Arguments, ConfigAndPacts}
import com.itv.scalapactcore.{Interaction, Pact, InteractionResponse, InteractionRequest}

import com.itv.scalapact.plugin.common.InteractionMatchers._

import scalaj.http.{HttpResponse, Http}
import scalaz.{-\/, \/-, \/}

object Verifier {

  lazy val verify: ConfigAndPacts => Unit = configAndPacts => {

    val pactVerifyResults = configAndPacts.pacts.map { pact =>
      PactVerifyResult(
        pact = pact,
        results = pact.interactions.map { interaction =>
          (doRequest(configAndPacts.arguments) andThen matchResponse(List(interaction)))(interaction.request)
        }
      )
    }

    pactVerifyResults.foreach { result =>
      println("Results for pact between " + result.pact.consumer + " and " + result.pact.provider)
      result.results.foreach {
        case \/-(r) => println(" - [  OK  ] " + r.description)
        case -\/(l) => println(" - [FAILED] " + l)
      }
    }

    ()
  }

  lazy val doRequest: Arguments => InteractionRequest => InteractionResponse = arguments => interactionRequest => {
    val baseUrl = "http://" + arguments.host + ":" + arguments.port

    try {
      InteractionRequest.unapply(interactionRequest) match {
        case Some((Some(method), Some(path), _, _)) =>

          httpResponseToInteractionResponse {
            val basicRequest = Http(baseUrl + path)

            val withData =
              if (interactionRequest.body.isDefined) basicRequest.postData(interactionRequest.body.get)
              else basicRequest

            val withHeaders =
              if (interactionRequest.headers.isDefined) withData.headers(interactionRequest.headers.getOrElse(Map.empty[String, String]))
              else withData

            val withMethod =
              withHeaders.method(method)

            withMethod.asString
          }

        case _ => InteractionResponse(None, None, None)

      }
    } catch {
      case e: Throwable =>
        println("!!!!!!!!")
        println("Error making request " + interactionRequest)
        println("!!!!!!!!")
        // I'm ok with just blowing up here at the moment...
        throw e
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
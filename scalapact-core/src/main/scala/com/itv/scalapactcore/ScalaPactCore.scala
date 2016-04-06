package com.itv.scalapactcore

import argonaut._
import Argonaut._

import scalaz._
import Scalaz._


object PactImplicits {
  implicit lazy val PactCodecJson: CodecJson[Pact] = casecodec3(Pact.apply, Pact.unapply)(
    "provider", "consumer", "interactions"
  )

  implicit lazy val PactActorCodecJson: CodecJson[PactActor] = casecodec1(PactActor.apply, PactActor.unapply)(
    "name"
  )

  implicit lazy val InteractionCodecJson: CodecJson[Interaction] = casecodec4(Interaction.apply, Interaction.unapply)(
    "providerState", "description", "request", "response"
  )

  implicit lazy val InteractionRequestCodecJson: CodecJson[InteractionRequest] = casecodec5(InteractionRequest.apply, InteractionRequest.unapply)(
    "method", "path", "query", "headers", "body"
  )

  implicit lazy val InteractionResponseCodecJson: CodecJson[InteractionResponse] = casecodec3(InteractionResponse.apply, InteractionResponse.unapply)(
    "status", "headers", "body"
  )
}

case class Pact(provider: PactActor, consumer: PactActor, interactions: List[Interaction])
case class PactActor(name: String)
case class Interaction(providerState: Option[String], description: String, request: InteractionRequest, response: InteractionResponse)
case class InteractionRequest(method: Option[String], path: Option[String], query: Option[String], headers: Option[Map[String, String]], body: Option[String]) {
  def unapply: Option[(Option[String], Option[String], Option[String], Option[Map[String, String]], Option[String])] = Some {
    (method, path, query, headers, body)
  }
}
case class InteractionResponse(status: Option[Int], headers: Option[Map[String, String]], body: Option[String])

object ScalaPactReader {

  import PactImplicits._

  val jsonStringToPact: String => \/[String, Pact] = json =>
    json.decodeEither[Pact]

  val rubyJsonToPact: String => String \/ Pact = json => {
    val brokenPact: Option[(PactActor, PactActor, List[(Option[Interaction], Option[String], Option[String])])] = for {
      provider <- RubyJsonHelper.extractPactActor("provider")(json)
      consumer <- RubyJsonHelper.extractPactActor("consumer")(json)
      interactions <- RubyJsonHelper.extractInteractions(json)
    } yield (provider, consumer, interactions)

    brokenPact.map { bp =>

      val interactions = bp._3.collect { case (Some(i), r1, r2) =>
        i.copy(
          request = i.request.copy(body = r1),
          response = i.response.copy(body = r2)
        )
      }

      Pact(
        provider = bp._1,
        consumer = bp._2,
        interactions = interactions
      )

    } match {
      case Some(pact) => pact.right
      case None => s"Could not read pact from json: $json".left
    }
  }

}

object ScalaPactWriter {

  import PactImplicits._

  val pactToJsonString: Pact => String = pact =>
    pact.asJson.pretty(PrettyParams.spaces2.copy(dropNullKeys = true))

  val pactToRubyJsonString: Pact => String = pact => {

    val interactions: JsonArray = pact.interactions.map { i =>

      val maybeRequestBody = i.request.body.flatMap { rb =>
        rb.parseOption.orElse(Option(jString(rb)))
      }

      val maybeResponseBody = i.response.body.flatMap { rb =>
        rb.parseOption.orElse(Option(jString(rb)))
      }

      val bodilessInteraction = i.copy(
        request = i.request.copy(body = None),
        response = i.response.copy(body = None)
      ).asJson

      val withRequestBody = for {
        rb <- maybeRequestBody
        aa <- bodilessInteraction.cursor.downField("request")
        bb <- aa.downField("body")
        cc <- bb.set(rb).some
      } yield cc.undo

      val withResponseBody = for {
        rb <- maybeResponseBody
        aa <- withRequestBody.flatMap(_.cursor.downField("response"))
        bb <- aa.downField("body")
        cc <- bb.set(rb).some
      } yield cc.undo

      withResponseBody
    }.collect { case Some(s) => s }

    val pactNoInteractionsAsJson = pact.copy(interactions = Nil).asJson

    val json = for {
      aa <- pactNoInteractionsAsJson.cursor.downField("interactions")
      bb <- aa.withFocus(_.withArray(p => interactions)).some
    } yield bb.undo

    json.getOrElse(throw new Exception("Something went really wrong serialising the following pact into json: " + pact)).pretty(PrettyParams.spaces2.copy(dropNullKeys = true))
  }

}

object RubyJsonHelper {

  import PactImplicits._

  val extractPactActor: String => String => Option[PactActor] = field => json => {

    val providerLens = jObjectPL >=> jsonObjectPL(field)

    val b = json.parseOption.flatMap(j => providerLens.get(j))

    b.flatMap(p => p.toString.decodeOption[PactActor])

  }

  val extractInteractions: String => Option[List[(Option[Interaction], Option[String], Option[String])]] = json => {

    val interactionsLens = jObjectPL >=> jsonObjectPL("interactions") >=> jArrayPL
    val requestBodyLensString = jObjectPL >=> jsonObjectPL("request") >=> jObjectPL >=> jsonObjectPL("body") >=> jStringPL
    val responseBodyLensString = jObjectPL >=> jsonObjectPL("response") >=> jObjectPL >=> jsonObjectPL("body") >=> jStringPL
    val requestBodyLensObject = jObjectPL >=> jsonObjectPL("request") >=> jObjectPL >=> jsonObjectPL("body")
    val responseBodyLensObject = jObjectPL >=> jsonObjectPL("response") >=> jObjectPL >=> jsonObjectPL("body")

    val interactions = json.parseOption.flatMap(j => interactionsLens.get(j))

    interactions.map { is =>
      is.map { i =>
        val minusRequestBody = for {
          aa <- i.cursor.downField("request")
          bb <- aa.downField("body")
          cc <- bb.delete
        } yield cc.undo

        val minusResponseBody = for {
          aa <- minusRequestBody.flatMap(ii => ii.cursor.downField("response"))
          bb <- aa.downField("body")
          cc <- bb.delete
        } yield cc.undo

        val requestBody = requestBodyLensString.get(i).orElse(requestBodyLensObject.get(i)).map(_.toString)
        val responseBody = responseBodyLensString.get(i).orElse(responseBodyLensObject.get(i)).map(_.toString)

        (minusResponseBody.flatMap(p => p.toString.decodeOption[Interaction]), requestBody, responseBody)
      }
    }
  }

}


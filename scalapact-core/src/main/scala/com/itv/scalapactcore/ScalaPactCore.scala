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

  implicit lazy val InteractionRequestCodecJson: CodecJson[InteractionRequest] = casecodec6(InteractionRequest.apply, InteractionRequest.unapply)(
    "method", "path", "query", "headers", "body", "matchingRules"
  )

  implicit lazy val InteractionResponseCodecJson: CodecJson[InteractionResponse] = casecodec4(InteractionResponse.apply, InteractionResponse.unapply)(
    "status", "headers", "body", "matchingRules"
  )

  implicit lazy val MatchingRuleCodecJson: CodecJson[MatchingRule] = casecodec2(MatchingRule.apply, MatchingRule.unapply)(
    "match", "regex"
  )
}

case class Pact(provider: PactActor, consumer: PactActor, interactions: List[Interaction])
case class PactActor(name: String)
case class Interaction(providerState: Option[String], description: String, request: InteractionRequest, response: InteractionResponse)
case class InteractionRequest(method: Option[String], path: Option[String], query: Option[String], headers: Option[Map[String, String]], body: Option[String], matchingRules: Option[Map[String, MatchingRule]]) {
  def unapply: Option[(Option[String], Option[String], Option[String], Option[Map[String, String]], Option[String])] = Some {
    (method, path, query, headers, body)
  }

  def cloneWithMaybeBody(maybeBody: Option[String]): InteractionRequest = this.copy(body = maybeBody)
}
case class InteractionResponse(status: Option[Int], headers: Option[Map[String, String]], body: Option[String], matchingRules: Option[Map[String, MatchingRule]]) {
  def cloneWithMaybeBody(maybeBody: Option[String]): InteractionResponse = this.copy(body = maybeBody)
}

case class MatchingRule(`match`: String, regex: Option[String])

object ScalaPactReader {

  val jsonStringToPact: String => String \/ Pact = json => {
    val brokenPact: Option[(PactActor, PactActor, List[(Option[Interaction], Option[String], Option[String])])] = for {
      provider <- JsonBodySpecialCaseHelper.extractPactActor("provider")(json)
      consumer <- JsonBodySpecialCaseHelper.extractPactActor("consumer")(json)
      interactions <- JsonBodySpecialCaseHelper.extractInteractions(json)
    } yield (provider, consumer, interactions)

    brokenPact.map { bp =>

      val interactions = bp._3.collect {
        case (Some(i), r1, r2) =>
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

  val pactToJsonString: Pact => String = pact => {

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

      val withRequestBody = {
        for {
          requestBody <- maybeRequestBody
          requestField <- bodilessInteraction.cursor.downField("request")
          bodyField <- requestField.downField("body")
          updated <- bodyField.set(requestBody).some
        } yield updated.undo
      } match {
        case ok @ Some(s) => ok
        case None => Option(bodilessInteraction) // There wasn't a body, but there was still an interaction.
      }

      val withResponseBody = {
        for {
          responseBody <- maybeResponseBody
          responseField <- withRequestBody.flatMap(_.cursor.downField("response"))
          bodyField <- responseField.downField("body")
          updated <- bodyField.set(responseBody).some
        } yield updated.undo
      } match {
        case ok @ Some(s) => ok
        case None => withRequestBody // There wasn't a body, but there was still an interaction.
      }

      withResponseBody
    }.collect { case Some(s) => s }

    val pactNoInteractionsAsJson = pact.copy(interactions = Nil).asJson

    val json = for {
      interactionsField <- pactNoInteractionsAsJson.cursor.downField("interactions")
      updated <- interactionsField.withFocus(_.withArray(p => interactions)).some
    } yield updated.undo

    // I don't believe you can ever see this exception.
    json
      .getOrElse(throw new Exception("Something went really wrong serialising the following pact into json: " + pact))
      .pretty(PrettyParams.spaces2.copy(dropNullKeys = true))
  }

}

object JsonBodySpecialCaseHelper {

  import PactImplicits._

  val extractPactActor: String => String => Option[PactActor] = field => json =>
    json
      .parseOption
      .flatMap { j => (j.hcursor --\ field).focus }
      .flatMap(p => p.toString.decodeOption[PactActor])

  val extractInteractions: String => Option[List[(Option[Interaction], Option[String], Option[String])]] = json => {

    val interations =
      json.parseOption
        .flatMap { j => (j.hcursor --\ "interactions").focus.flatMap(_.array) }

    interations.map { is =>
      is.map { i =>
        val minusRequestBody =
          (i.hcursor --\ "request" --\ "body").delete.undo match {
            case ok @ Some(s) => ok
            case None => Option(i)
          }

        val minusResponseBody = minusRequestBody.flatMap { ii =>
          (ii.hcursor --\ "response" --\ "body").delete.undo match {
            case ok@Some(s) => ok
            case None => minusRequestBody // There wasn't a body, but there was still an interaction.
          }
        }

        val requestBody = (i.hcursor --\ "request" --\ "body").focus
          .flatMap { p =>
            if(p.isString) p.string.map(_.toString) else p.toString.some
          }

        val responseBody = (i.hcursor --\ "response" --\ "body").focus
          .flatMap { p =>
            if(p.isString) p.string.map(_.toString) else p.toString.some
          }

        (minusResponseBody.flatMap(p => p.toString.decodeOption[Interaction]), requestBody, responseBody)
      }
    }
  }

}


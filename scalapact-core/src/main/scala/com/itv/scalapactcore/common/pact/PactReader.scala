package com.itv.scalapactcore.common.pact

import argonaut._
import Argonaut._
import com.itv.scalapactcore._

object PactReader {

  val jsonStringToPact: String => Either[String, Pact] = json => {
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
          .map(i => Pact.providerStateReaderLens.set(i, Pact.providerStatePicker(i)))
          .map(i => Pact.providerStateWriterLens.set(i, None) )
      )

    } match {
      case Some(pact) => Right(pact)
      case None => Left(s"Could not read pact from json: $json")
    }
  }

}

object JsonBodySpecialCaseHelper {

  import Pact._

  val extractPactActor: String => String => Option[PactActor] = field => json =>
    json
      .parseOption
      .flatMap { j => (j.hcursor --\ field).focus }
      .flatMap(p => p.toString.decodeOption[PactActor])

  val extractInteractions: String => Option[List[(Option[Interaction], Option[String], Option[String])]] = json => {

    val interations =
      json.parseOption
        .flatMap { j => (j.hcursor --\ "interactions").focus.flatMap(_.array) }

    val makeOptionalBody: Json => Option[String] = j => j match {
      case body: Json if body.isString =>
        j.string.map(_.toString)

      case _ =>
        Option(j.toString)
    }

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
          .flatMap { makeOptionalBody }

        val responseBody = (i.hcursor --\ "response" --\ "body").focus
          .flatMap { makeOptionalBody }

        (minusResponseBody.flatMap(p => p.toString.decodeOption[Interaction]), requestBody, responseBody)
      }
    }
  }

}
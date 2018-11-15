package com.itv.scalapact.argonaut62

import argonaut.Argonaut._
import argonaut._
import com.itv.scalapact.shared.Pact.Links
import com.itv.scalapact.shared._
import com.itv.scalapact.shared.matchir.IrNode
import com.itv.scalapact.shared.typeclasses.IPactReader

class PactReader extends IPactReader {

  def fromJSON(jsonString: String): Option[IrNode] =
    JsonConversionFunctions.fromJSON(jsonString)

  def jsonStringToPact(json: String): Either[String, Pact] = {
    val brokenPact: Option[
      (PactActor,
       PactActor,
       List[(Option[Interaction], Option[String], Option[String])],
       Option[Links],
       Option[PactMetaData])
    ] =
      for {
        provider     <- JsonBodySpecialCaseHelper.extractPactActor("provider")(json)
        consumer     <- JsonBodySpecialCaseHelper.extractPactActor("consumer")(json)
        interactions <- JsonBodySpecialCaseHelper.extractInteractions(json)
      } yield
        (provider,
         consumer,
         interactions,
         JsonBodySpecialCaseHelper.extractLinks(json),
         JsonBodySpecialCaseHelper.extractPactMetaData(json))

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
          .map(i => i.copy(providerState = i.providerState.orElse(i.provider_state)))
          .map(i => i.copy(provider_state = None)),
        _links = bp._4,
        metadata = bp._5
      )

    } match {
      case Some(pact) => Right(pact)
      case None       => Left(s"Could not read pact from json: $json")
    }
  }

}

object JsonBodySpecialCaseHelper {

  import PactImplicits._

  val extractPactActor: String => String => Option[PactActor] = field =>
    json =>
      json.parseOption
        .flatMap { j =>
          (j.hcursor --\ field).focus
        }
        .flatMap(p => p.toString.decodeOption[PactActor])

  val extractInteractions: String => Option[List[(Option[Interaction], Option[String], Option[String])]] = json => {

    val interactions =
      json.parseOption
        .flatMap { j =>
          (j.hcursor --\ "interactions").focus.flatMap(_.array)
        }

    val makeOptionalBody: Json => Option[String] = {
      case body: Json if body.isString =>
        body.string.map(_.toString)

      case body =>
        Option(body.toString)
    }

    interactions.map { is =>
      is.map { i =>
        val minusRequestBody =
          (i.hcursor --\ "request" --\ "body").delete.undo match {
            case ok @ Some(_) => ok
            case None         => Option(i)
          }

        val minusResponseBody = minusRequestBody.flatMap { ii =>
          (ii.hcursor --\ "response" --\ "body").delete.undo match {
            case ok @ Some(_) => ok
            case None         => minusRequestBody // There wasn't a body, but there was still an interaction.
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

  @SuppressWarnings(Array("org.wartremover.warts.PublicInference", "org.wartremover.warts.Any"))
  val extractLinks: String => Option[Links] = json => {
    json.parseOption
      .flatMap { j =>
        (j.hcursor --\ "_links").focus
      }
      .flatMap { j =>
        val withoutCuries = (j.hcursor --\ "curies").delete.undo match {
          case ok @ Some(_) => ok
          case None         => Option(j)
        }

        withoutCuries.flatMap(_.toString.decodeOption[Links])
      }
  }

  @SuppressWarnings(Array("org.wartremover.warts.PublicInference"))
  val extractPactMetaData: String => Option[PactMetaData] =
    json =>
      json.parseOption
        .flatMap { j =>
          (j.hcursor --\ "metadata").focus
        }
        .flatMap(p => p.toString.decodeOption[PactMetaData])

}

package com.itv.scalapact.circe09

import io.circe._
import io.circe.parser._
import io.circe.generic.auto._
import com.itv.scalapact.shared._
import com.itv.scalapact.shared.matchir.IrNode
import com.itv.scalapact.shared.typeclasses.IPactReader

class PactReader extends IPactReader {

  def fromJSON(jsonString: String): Option[IrNode] =
    JsonConversionFunctions.fromJSON(jsonString)

  def jsonStringToPact(json: String): Either[String, Pact] = {
    val brokenPact: Option[(PactActor, PactActor, List[(Option[Interaction], Option[String], Option[String])])] = for {
      provider     <- JsonBodySpecialCaseHelper.extractPactActor("provider")(json)
      consumer     <- JsonBodySpecialCaseHelper.extractPactActor("consumer")(json)
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
          .map(i => i.copy(providerState = i.providerState.orElse(i.provider_state)))
          .map(i => i.copy(provider_state = None))
      )

    } match {
      case Some(pact) => Right(pact)
      case None       => Left(s"Could not read pact from json: $json")
    }
  }

}

object JsonBodySpecialCaseHelper {

  // Used by old Scala versions
  import EitherWithToOption._

  @SuppressWarnings(Array("org.wartremover.warts.PublicInference"))
  val extractPactActor: String => String => Option[PactActor] = field =>
    json =>
      parse(json).asOption
        .flatMap { j =>
          j.hcursor.downField(field).focus
        }
        .flatMap(p => p.as[PactActor].asOption)

  @SuppressWarnings(Array("org.wartremover.warts.PublicInference"))
  val extractInteractions: String => Option[List[(Option[Interaction], Option[String], Option[String])]] = json => {

    val interations =
      parse(json).asOption
        .flatMap { j =>
          j.hcursor.downField("interactions").focus.flatMap(p => p.asArray.map(_.toList))
        }

    val makeOptionalBody: Json => Option[String] = {
      case body: Json if body.isString =>
        body.asString

      case body =>
        Option(body.pretty(Printer.spaces2.copy(dropNullValues = true)))
    }

    interations.map { is =>
      is.map { i =>
        val minusRequestBody =
          i.hcursor.downField("request").downField("body").delete.top match {
            case ok @ Some(_) => ok
            case None         => Option(i)
          }

        val minusResponseBody = minusRequestBody.flatMap { ii =>
          ii.hcursor.downField("response").downField("body").delete.top match {
            case ok @ Some(_) => ok
            case None         => minusRequestBody // There wasn't a body, but there was still an interaction.
          }
        }

        val requestBody = i.hcursor
          .downField("request")
          .downField("body")
          .focus
          .flatMap { makeOptionalBody }

        val responseBody = i.hcursor
          .downField("response")
          .downField("body")
          .focus
          .flatMap { makeOptionalBody }

        (minusResponseBody.flatMap(p => p.as[Interaction].asOption), requestBody, responseBody)
      }
    }
  }

}

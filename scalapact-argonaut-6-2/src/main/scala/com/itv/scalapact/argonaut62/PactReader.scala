package com.itv.scalapact.argonaut62

import argonaut.Argonaut._
import argonaut._
import com.itv.scalapact.shared._
import com.itv.scalapact.shared.matchir.IrNode
import com.itv.scalapact.shared.typeclasses.IPactReader

class PactReader extends IPactReader {

  def fromJSON(jsonString: String): Option[IrNode] =
    JsonConversionFunctions.fromJSON(jsonString)

  def jsonStringToPact(json: String): Either[String, Pact] =
    (for {
      parsedJson   <- json.parse.fold(x => DecodeResult.fail(x, CursorHistory.empty), DecodeResult.ok)
      provider     <- JsonBodySpecialCaseHelper.extractPactActor("provider")(parsedJson)
      consumer     <- JsonBodySpecialCaseHelper.extractPactActor("consumer")(parsedJson)
      interactions <- JsonBodySpecialCaseHelper.extractInteractions(parsedJson)
      messages     <- JsonBodySpecialCaseHelper.extractMessages(parsedJson)
    } yield Pact(provider, consumer, interactions, messages))
      .fold(
        { case (message, h) => Left(s"$message: [${h.toString()}]") },
        Right(_)
      )

}

object JsonBodySpecialCaseHelper {

  import PactImplicits._

  type JsonParser[T]           = Json => DecodeResult[T]
  type InterationTuple         = (Interaction, Option[String], Option[String])
  type InteractionDecodeResult = DecodeResult[List[InterationTuple]]

  val extractPactActor: String => JsonParser[PactActor] = field =>
    json =>
      json
        .field(field)
        .fold[DecodeResult[PactActor]](DecodeResult.fail(s"field `$field` does not exist", CursorHistory.empty))(
          _.as[PactActor]
    )

  val extractInteractions: JsonParser[List[Interaction]] = json =>
    extractInteractionsTuple(json).map(_.map {
      case (interaction, request, response) =>
        interaction.copy(
          request = interaction.request.copy(body = request),
          response = interaction.response.copy(body = response),
          provider_state = None,
          providerState = interaction.providerState.orElse(interaction.provider_state)
        )

    })

  protected[argonaut62] val extractInteractionsTuple: JsonParser[List[InterationTuple]] = json =>
    json
      .field("interactions")
      .fold[DecodeResult[JsonArray]](DecodeResult.ok(List.empty[Json]))(
        _.array.fold[DecodeResult[JsonArray]](DecodeResult.fail("", CursorHistory.empty))(DecodeResult.ok)
      )
      .flatMap {
        _.map(parseInteractionTuple)
          .foldLeft[InteractionDecodeResult](DecodeResult.ok(List.empty)) { (acc, next) =>
            next.fold(
              DecodeResult.fail,
              x =>
                if (acc.isError)
                  acc
                else
                  x.fold(acc)(y => acc.map(_.:+(y)))
            )
          }
    }

  private def parseInteractionTuple: JsonParser[Option[InterationTuple]] = json => {
    val minusRequestBody =
      (json.hcursor --\ "request" --\ "body").delete.undo match {
        case ok @ Some(_) => ok
        case None         => Option(json)
      }

    val minusResponseBody = minusRequestBody.flatMap { ii =>
      (ii.hcursor --\ "response" --\ "body").delete.undo match {
        case ok @ Some(_) => ok
        case None         => minusRequestBody // There wasn't a body, but there was still an interaction.
      }
    }

    val requestBody = (json.hcursor --\ "request" --\ "body").focus
      .flatMap {
        makeOptionalBody
      }

    val responseBody = (json.hcursor --\ "response" --\ "body").focus
      .flatMap {
        makeOptionalBody
      }

    minusResponseBody
      .map(_.as[Interaction])
      .map(dr => dr.map(x => Option((x, requestBody, responseBody))))
      .getOrElse(DecodeResult.ok(None))
  }

  val extractMessages: Json => DecodeResult[List[Message]] = json =>
    json
      .field("messages")
      .fold[DecodeResult[List[Message]]](DecodeResult.ok(List.empty[Message]))(_.as[List[Message]])

  private val makeOptionalBody: Json => Option[String] = {
    case body: Json if body.isString =>
      body.string.map(_.toString)

    case body =>
      Option(body.toString)
  }

}

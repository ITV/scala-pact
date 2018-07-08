package com.itv.scalapact.circe09

import com.itv.scalapact.shared._
import com.itv.scalapact.shared.matchir.IrNode
import com.itv.scalapact.shared.typeclasses.IPactReader
import io.circe._
import io.circe.parser._

class PactReader extends IPactReader {

  def fromJSON(jsonString: String): Option[IrNode] =
    JsonConversionFunctions.fromJSON(jsonString)

  def jsonStringToPact(json: String): Either[String, Pact] =
    (for {
      parsedJson   <- parse(json)
      provider     <- JsonBodySpecialCaseHelper.extractPactActor("provider")(parsedJson)
      consumer     <- JsonBodySpecialCaseHelper.extractPactActor("consumer")(parsedJson)
      interactions <- JsonBodySpecialCaseHelper.extractInteractions(parsedJson)
      messages     <- JsonBodySpecialCaseHelper.extractMessages(parsedJson)
    } yield Pact(provider, consumer, interactions, messages))
      .fold(
        x => Left(s"There was a failure ${x.getMessage}: [$x]"),
        Right(_)
      )
}

object JsonBodySpecialCaseHelper {
  import PactImplicits._

  type JsonParser[T]                = Json => Decoder.Result[T]
  type InterationTuple              = (Interaction, Option[String], Option[String])
  type InteractionTupleDecodeResult = Decoder.Result[List[InterationTuple]]
  @SuppressWarnings(Array("org.wartremover.warts.PublicInference"))
  val extractPactActor: String => JsonParser[PactActor] = field =>
    json =>
      json.asObject
        .flatMap(_(field).map(_.as[PactActor]))
        .getOrElse(fail(s"field `$field` does not exist"))

  @SuppressWarnings(Array("org.wartremover.warts.PublicInference", "org.wartremover.warts.Any"))
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

  protected[circe09] val extractInteractionsTuple: JsonParser[List[InterationTuple]] = json => {
    json.asObject
      .flatMap(_("interactions"))
      .fold(suceed(Vector.empty[Json]))(
        _.asArray.fold(fail[Vector[Json]](s"`interactions` field is not an array: [$json]"))(suceed)
      )
      .flatMap {
        _.map(parseInteractionTuple)
          .foldLeft(suceed(List.empty[InterationTuple])) { (acc: InteractionTupleDecodeResult, next) =>
            next.fold(
              fail[List[InterationTuple]],
              x =>
                acc.fold[InteractionTupleDecodeResult](
                  _ => acc,
                  currentList => x.fold(acc)(nextTuple => suceed(currentList ++ List(nextTuple)))
              )
            )
          }
      }
  }

  private def parseInteractionTuple: JsonParser[Option[InterationTuple]] = json => {
    val minusRequestBody =
      json.hcursor.downField("request").downField("body").delete.top match {
        case ok @ Some(_) => ok
        case None         => Option(json)
      }

    val minusResponseBody = minusRequestBody.flatMap { ii =>
      ii.hcursor.downField("response").downField("body").delete.top match {
        case ok @ Some(_) => ok
        case None         => minusRequestBody // There wasn't a body, but there was still an interaction.
      }
    }

    val requestBody = json.hcursor
      .downField("request")
      .downField("body")
      .focus
      .flatMap {
        makeOptionalBody
      }

    val responseBody = json.hcursor
      .downField("response")
      .downField("body")
      .focus
      .flatMap {
        makeOptionalBody
      }

    minusResponseBody
      .map(_.as[Interaction])
      .map(dr => dr.map(x => Option((x, requestBody, responseBody))))
      .getOrElse(suceed(None))
  }
  @SuppressWarnings(Array("org.wartremover.warts.PublicInference", "org.wartremover.warts.Any"))
  val extractMessages: JsonParser[List[Message]] = _.asObject
    .flatMap(_("messages"))
    .map(_.as[List[Message]])
    .getOrElse(suceed(List.empty[Message]))

  private val makeOptionalBody: Json => Option[String] = {
    case body: Json if body.isString =>
      body.asString

    case body =>
      Option(body.toString)
  }

  private def fail[T](value: DecodingFailure): Either[DecodingFailure, T] =
    Left(value)

  private def fail[T](value: String): Either[DecodingFailure, T] =
    Left(DecodingFailure(value, List.empty))

  private def suceed[T](value: T): Either[DecodingFailure, T] =
    Right(value)

}

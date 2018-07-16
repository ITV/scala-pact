package com.itv.scalapact.circe09

import com.itv.scalapact.shared.Message.Metadata
import com.itv.scalapact.shared.MessageContentType.ApplicationJson
import com.itv.scalapact.shared._
import io.circe._
import io.circe.generic.semiauto._
import io.circe.parser.parse
import io.circe.syntax._

@SuppressWarnings(Array("org.wartremover.warts.Any", "org.wartremover.warts.PublicInference"))
object PactImplicits {
  implicit val pactActorEncoder: Encoder[PactActor] = deriveEncoder[PactActor]
  implicit val pactActorDecoder: Decoder[PactActor] = deriveDecoder[PactActor]

  implicit val messageMatchersEncoder: Encoder[Message.Matchers] = deriveEncoder[Message.Matchers]
  implicit val messageMatchersDecoder: Decoder[Message.Matchers] = deriveDecoder[Message.Matchers]

  implicit val matchingRulesEncoder: Encoder[MatchingRule] = deriveEncoder[MatchingRule]
  implicit val matchingRulesDecoder: Decoder[MatchingRule] = deriveDecoder[MatchingRule]

  implicit val interactionRequestEncoder: Encoder[InteractionRequest] = deriveEncoder[InteractionRequest]
  implicit val interactionRequestDecoder: Decoder[InteractionRequest] = deriveDecoder[InteractionRequest]

  implicit val InteractionResponseEncoder: Encoder[InteractionResponse] = deriveEncoder[InteractionResponse]
  implicit val InteractionResponseDecoder: Decoder[InteractionResponse] = deriveDecoder[InteractionResponse]

  implicit val interactionEncoder: Encoder[Interaction] = deriveEncoder[Interaction]
  implicit val interactionDecoder: Decoder[Interaction] = deriveDecoder[Interaction]

  implicit val providerStateEncoder: Encoder[ProviderState] = Encoder.instance( ps =>
    Json.obj(
      "name" -> ps.name.asJson,
      "params" -> (ps.params match {
        case params if params.nonEmpty => params.asJson
        case _ => Json.Null
      })
    )
  )
  implicit val providerStateDecoder: Decoder[ProviderState] = Decoder.instance(c => for {
    name <- c.downField("name").as[String]
    params <- c.downField("params").as[Option[Map[String,String]]]
  } yield ProviderState(name, params.getOrElse(Map.empty)))

  implicit val messageEncoder: Encoder[Message] = Encoder.instance { m =>
    Json.obj(
      "description"    -> m.description.asJson,
      "providerStates" -> m.providerStates.asJson,
      "contents" -> (m.contentType match {
        case ApplicationJson => parse(m.contents).toOption.getOrElse(Json.Null)
        case _               => Json.fromString(m.contents)
      }),
      "metaData" -> m.metaData.asJson,
      "matchingRules" -> (m.matchingRules match {
        case mr if mr.nonEmpty => mr.asJson
        case _                 => Json.Null
      })
    )
  }
  @SuppressWarnings(Array("org.wartremover.warts.Any"))
  implicit val messageDecoder: Decoder[Message] = Decoder.decodeJson.emap { json =>
    val cursor = json.hcursor
    (for {
      description    <- cursor.downField("description").as[String]
      providerState  <- cursor.downField("providerState").as[Option[String]]
      providerStates <- cursor.downField("providerStates").as[Option[List[ProviderState]]]
      contents       <- contents(cursor)
      metaData       <- cursor.downField("metaData").as[Metadata]
      matchingRules  <- cursor.downField("matchingRules").as[Option[Message.MatchingRules]]
    } yield
      Message(
        description,
        providerState.toList.map(s =>ProviderState(s,Map.empty)) ++ providerStates.toList.flatten,
        contents.asString.getOrElse(contents.noSpaces),
        metaData,
        matchingRules.getOrElse(Map.empty),
        contentType(contents)
      ))
      .fold(
        f => Left(s"There was a failure during decoder because ${f.message}: [$f]"),
        Right(_)
      )
  }

  def contentType(contents: Json): MessageContentType =
    contents
      .as[String]
      .map(_ => MessageContentType.ApplicationText)
      .toOption
      .getOrElse[MessageContentType](MessageContentType.ApplicationJson)

  private def contents(cursor: HCursor): Decoder.Result[Json] =
    cursor
      .downField("contents")
      .as[Json]

  implicit val pactEncoder: Encoder[Pact] = deriveEncoder[Pact]
  implicit val pactDecoder: Decoder[Pact] = deriveDecoder[Pact]
}

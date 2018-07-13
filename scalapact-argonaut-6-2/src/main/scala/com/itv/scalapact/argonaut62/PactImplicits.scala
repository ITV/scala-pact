package com.itv.scalapact.argonaut62

import argonaut.Argonaut._
import argonaut._
import com.itv.scalapact.shared._
import argonaut._
import Argonaut._

import com.itv.scalapact.shared.MessageContentType.ApplicationJson

object PactImplicits {
  implicit lazy val PactCodecJson: CodecJson[Pact] = casecodec4(Pact.apply, Pact.unapply)(
    "provider",
    "consumer",
    "interactions",
    "messages"
  )

  implicit lazy val PactActorCodecJson: CodecJson[PactActor] = casecodec1(PactActor.apply, PactActor.unapply)(
    "name"
  )

  private def contents(message: Message): Json = message.contentType match {
    case ApplicationJson => message.contents.value.parse.fold(_ => Json.jNull, identity)
    case _               => message.contents.asJson
  }

  implicit lazy val MessageMatchersCodecJson: CodecJson[Message.Matchers] =
    casecodec1(Message.Matchers.apply, Message.Matchers.unapply)(
      "matchers"
    )

  implicit lazy val MessageCodecJson: CodecJson[Message] = CodecJson(
    message =>
      Json.obj(
        "description"    -> message.description.asJson,
        "providerStates" -> message.providerStates.asJson,
        "contents"       -> contents(message),
        "metaData"       -> message.metaData.asJson,
        "matchingRules" -> (message.matchingRules match {
          case mr if mr.nonEmpty => message.matchingRules.asJson
          case _                 => jNull
        })
    ),
    c =>
      for {
        description    <- (c --\ "description").as[String]
        providerState  <- (c --\ "providerState").as[Option[String]].map(_.toList)
        providerStates <- (c --\ "providerStates").as[Option[List[String]]].map(_.toList.flatten)
        contents       <- (c --\ "contents").as[Json]
        metadata       <- (c --\ "metaData").as[Map[String, String]]
        matchingRules  <- (c --\ "matchingRules").as[Option[Message.MatchingRules]]
      } yield
        Message(description,
                providerState ++ providerStates,
                contents.nospaces,
                metadata,
                matchingRules.getOrElse(Map.empty),
                contentType(contents))
  )

  def contentType(contents: Json): MessageContentType =
    contents
      .as[String]
      .fold[MessageContentType]((_, _) => MessageContentType.ApplicationJson, _ => MessageContentType.ApplicationText)

  implicit lazy val MessageContentTypeCodecJson: CodecJson[MessageContentType] = CodecJson[MessageContentType](
    x => EncodeJson.StringEncodeJson(x.renderString),
    _.as[String].map(MessageContentType.apply)
  )

  implicit lazy val InteractionCodecJson: CodecJson[Interaction] = casecodec5(Interaction.apply, Interaction.unapply)(
    "provider_state",
    "providerState",
    "description",
    "request",
    "response"
  )

  implicit lazy val InteractionRequestCodecJson: CodecJson[InteractionRequest] =
    casecodec6(InteractionRequest.apply, InteractionRequest.unapply)(
      "method",
      "path",
      "query",
      "headers",
      "body",
      "matchingRules"
    )

  implicit lazy val InteractionResponseCodecJson: CodecJson[InteractionResponse] =
    casecodec4(InteractionResponse.apply, InteractionResponse.unapply)(
      "status",
      "headers",
      "body",
      "matchingRules"
    )

  implicit lazy val MatchingRuleCodecJson: CodecJson[MatchingRule] =
    casecodec3(MatchingRule.apply, MatchingRule.unapply)(
      "match",
      "regex",
      "min"
    )

}

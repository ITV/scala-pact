package com.itv.scalapact.argonaut62

import argonaut.Argonaut._
import argonaut._
import com.itv.scalapact.shared._
import argonaut._, Argonaut._

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

  implicit lazy val MessageCodecJson: CodecJson[Message] = CodecJson(
    message => ???, //FIXME Implement it
    c =>
      for {
        description   <- (c --\ "description").as[String]
        providerState <- (c --\ "providerState").as[Option[String]]
        contents      <- (c --\ "contents").as[Json].map(_.nospaces)
        metadata      <- (c --\ "metaData").as[Map[String, String]]
      } yield Message(description, providerState, contents, metadata)
  )
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

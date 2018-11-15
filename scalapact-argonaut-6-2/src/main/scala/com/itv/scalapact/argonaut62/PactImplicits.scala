package com.itv.scalapact.argonaut62

import argonaut.Argonaut._
import argonaut._
import com.itv.scalapact.shared._

object PactImplicits {

  implicit lazy val LinkValuesCodecJson: CodecJson[LinkValues] = casecodec4(LinkValues.apply, LinkValues.unapply)(
    "title",
    "name",
    "href",
    "templated"
  )

  implicit lazy val PactCodecJson: CodecJson[Pact] = casecodec5(Pact.apply, Pact.unapply)(
    "provider",
    "consumer",
    "interactions",
    "_links",
    "metadata"
  )

  implicit lazy val PactActorCodecJson: CodecJson[PactActor] = casecodec1(PactActor.apply, PactActor.unapply)(
    "name"
  )

  implicit lazy val PactMetaDataCodecJson: CodecJson[PactMetaData] =
    casecodec2(PactMetaData.apply, PactMetaData.unapply)(
      "pactSpecification",
      "scala-pact"
    )

  implicit lazy val VersionMetaDataCodecJson: CodecJson[VersionMetaData] =
    casecodec1(VersionMetaData.apply, VersionMetaData.unapply)(
      "version"
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

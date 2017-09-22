package com.itv.scalapact.shared

import argonaut.Argonaut._
import argonaut._
import com.itv.scalapactcore.common.pact._

object PactImplicits {
  implicit lazy val PactCodecJson: CodecJson[Pact] = casecodec3(Pact.apply, Pact.unapply)(
    "provider", "consumer", "interactions"
  )

  implicit lazy val PactActorCodecJson: CodecJson[PactActor] = casecodec1(PactActor.apply, PactActor.unapply)(
    "name"
  )

  implicit lazy val InteractionCodecJson: CodecJson[Interaction] = casecodec5(Interaction.apply, Interaction.unapply)(
    "provider_state", "providerState", "description", "request", "response"
  )

  implicit lazy val InteractionRequestCodecJson: CodecJson[InteractionRequest] = casecodec6(InteractionRequest.apply, InteractionRequest.unapply)(
    "method", "path", "query", "headers", "body", "matchingRules"
  )

  implicit lazy val InteractionResponseCodecJson: CodecJson[InteractionResponse] = casecodec4(InteractionResponse.apply, InteractionResponse.unapply)(
    "status", "headers", "body", "matchingRules"
  )

  implicit lazy val MatchingRuleCodecJson: CodecJson[MatchingRule] = casecodec3(MatchingRule.apply, MatchingRule.unapply)(
    "match", "regex", "min"
  )

}
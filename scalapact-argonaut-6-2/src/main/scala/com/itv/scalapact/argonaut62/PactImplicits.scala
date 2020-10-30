package com.itv.scalapact.argonaut62

import argonaut.Argonaut._
import argonaut._
import com.itv.scalapact.shared._
import argonaut.Parse
import com.itv.scalapact.shared.Pact.Links

object PactImplicits {

  implicit lazy val LinkValuesCodecJson: CodecJson[LinkValues] = casecodec4(LinkValues.apply, LinkValues.unapply)(
    "title",
    "name",
    "href",
    "templated"
  )

  implicit lazy val pactDecodeJson: DecodeJson[Pact] = DecodeJson[Pact] { cur =>
    for {
      provider <- cur.get[PactActor]("provider")
      consumer <- cur.get[PactActor]("consumer")
      interactions <- cur.get[List[Interaction]]("interactions")
      _links <- cur.downField("_links").downField("curies").delete.as[Option[Links]]
      metadata <- cur.get[Option[PactMetaData]]("metadata")
    } yield Pact(provider, consumer, interactions, _links, metadata)
  }

  implicit lazy val PactEncodeJson: EncodeJson[Pact] = EncodeJson { p =>
    Json.obj(
    "provider" -> p.provider.asJson,
    "consumer" -> p.consumer.asJson,
    "interactions" -> p.interactions.asJson,
    "_links" -> p._links.asJson,
    "metadata" -> p.metadata.asJson
    )
  }

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

  implicit lazy val InteractionRequestDecodeJson: DecodeJson[InteractionRequest] = DecodeJson[InteractionRequest] { cur =>
    val body = cur.downField("body").focus.flatMap {
      case j if j.isString => j.as[String].toOption
      case j => Some(j.pretty(PrettyParams.spaces2.copy(dropNullKeys = true)))
    }
    for {
      method <- cur.get[Option[String]]("method")
      path <- cur.get[Option[String]]("path")
      query <- cur.get[Option[String]]("query")
      headers <- cur.get[Option[Map[String, String]]]("headers")
      matchingRules <- cur.get[Option[Map[String, MatchingRule]]]("matchingRules")
    } yield InteractionRequest(method, path, query, headers, body, matchingRules)
  }

  implicit lazy val interactionRequestEncodeJson: EncodeJson[InteractionRequest] = EncodeJson[InteractionRequest] { r =>
    val encodedBody: Option[Json] = r.body.map(b => Parse.parse(b).toOption.getOrElse(Json.jString(b)))
    Json.obj(
      "method" -> r.method.asJson,
      "path" -> r.path.asJson,
      "query" -> r.query.asJson,
      "headers" -> r.headers.asJson,
      "body" -> encodedBody.asJson,
      "matchingRules" -> r.matchingRules.asJson
    )
  }

  implicit lazy val InteractionResponseDecodeJson: DecodeJson[InteractionResponse] = DecodeJson[InteractionResponse] { cur =>
    val body = cur.downField("body").focus.flatMap {
      case j if j.isString => j.as[String].toOption
      case j => Some(j.pretty(PrettyParams.spaces2.copy(dropNullKeys = true)))
    }
    for {
      status <- cur.get[Option[Int]]("status")
      headers <- cur.get[Option[Map[String, String]]]("headers")
      matchingRules <- cur.get[Option[Map[String, MatchingRule]]]("matchingRules")
    } yield InteractionResponse(status, headers, body, matchingRules)
  }

  implicit lazy val InteractionResponseEncodeJson: EncodeJson[InteractionResponse] = EncodeJson[InteractionResponse] { r =>
    val encodedBody: Option[Json] = r.body.map(b => Parse.parse(b).toOption.getOrElse(Json.jString(b)))
    Json.obj(
      "status" -> r.status.asJson,
      "headers" -> r.headers.asJson,
      "body" -> encodedBody.asJson,
      "matchingRules" -> r.matchingRules.asJson
    )
  }

  implicit lazy val MatchingRuleCodecJson: CodecJson[MatchingRule] =
    casecodec3(MatchingRule.apply, MatchingRule.unapply)(
      "match",
      "regex",
      "min"
    )

  implicit lazy val interactionDecodeJson: DecodeJson[Interaction] = DecodeJson[Interaction] { cur =>
    for {
      providerState <- cur.get[Option[String]]("providerState")
      provider_state <- cur.get[Option[String]]("provider_state")
      description <- cur.get[String]("description")
      request <- cur.get[InteractionRequest]("request")
      response <- cur.get[InteractionResponse]("response")
    } yield Interaction(providerState.orElse(provider_state), description, request, response)
  }

  implicit lazy val InteractionCodecJson: EncodeJson[Interaction] = EncodeJson[Interaction] { i =>
    Json.obj(
      "providerState" -> i.providerState.asJson,
      "description" -> i.description.asJson,
      "request" -> i.request.asJson,
      "response" -> i.response.asJson
    )
  }

  implicit lazy val halIndexDecoder: DecodeJson[HALIndex] =
    DecodeJson[HALIndex](_.downField("_links").downField("curies").delete.as[Links].map(HALIndex))

  implicit lazy val verificationPropertiesDecodeJson: DecodeJson[VerificationProperties] =
    DecodeJson[VerificationProperties](_.downField("notices").as[List[Map[String, String]]].map(VerificationProperties))

  implicit lazy val embeddedPactForVerificationDecodeJson: DecodeJson[EmbeddedPactForVerification] = DecodeJson[EmbeddedPactForVerification] { cur =>
    for {
      vp <- cur.downField("verificationProperties").as[VerificationProperties]
      links <- cur.downField("_links").as[Links]
    } yield EmbeddedPactForVerification(vp, links)
  }

  implicit lazy val pactsForVerificationDecoder: DecodeJson[PactsForVerificationResponse] = DecodeJson[PactsForVerificationResponse]{ cur =>
    for {
      pacts <- cur.downField("_embedded").downField("pacts").as[List[EmbeddedPactForVerification]]
      links <- cur.downField("_links").as[Links]
    } yield PactsForVerificationResponse(EmbeddedPactsForVerification(pacts), links)
  }

  implicit lazy val consumerVersionSelectorEncodeJson: EncodeJson[ConsumerVersionSelector] = EncodeJson[ConsumerVersionSelector]{ vs =>
    Json.obj(
      "tag" -> vs.tag.asJson,
      "fallbackTag" -> vs.fallbackTag.asJson,
      "consumer" -> vs.consumer.asJson,
      "latest" -> vs.latest.asJson
    )
  }

  implicit lazy val pactsForVerificationRequestEncoder: EncodeJson[PactsForVerificationRequest] = EncodeJson[PactsForVerificationRequest]{ r =>
    Json.obj(
      "consumerVersionSelectors" -> r.consumerVersionSelectors.asJson,
      "providerVersionTags" -> r.providerVersionTags.asJson
    )
  }

}

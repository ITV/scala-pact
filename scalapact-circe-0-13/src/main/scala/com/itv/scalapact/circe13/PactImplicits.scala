package com.itv.scalapact.circe13

import com.itv.scalapact.shared.Pact.Links
import com.itv.scalapact.shared.{ConsumerVersionSelector, EmbeddedPactForVerification, EmbeddedPactsForVerification, HALIndex, Interaction, InteractionRequest, InteractionResponse, LinkValues, MatchingRule, Pact, PactActor, PactMetaData, PactsForVerificationRequest, PactsForVerificationResponse, VerificationProperties, VersionMetaData}
import io.circe.{Codec, Decoder, Encoder, Json}
import io.circe.generic.semiauto.{deriveCodec, deriveDecoder, deriveEncoder}
import io.circe.syntax._
import io.circe.parser

object PactImplicits {

  implicit val pactActorDecoder: Codec[PactActor] = deriveCodec

  implicit val matchingRuleDecoder: Codec[MatchingRule] = deriveCodec

  implicit val interactionRequestDecoder: Decoder[InteractionRequest] = Decoder.instance { cur =>
    for {
      method <- cur.get[Option[String]]("method")
      path <- cur.get[Option[String]]("path")
      query <- cur.get[Option[String]]("query")
      headers <- cur.get[Option[Map[String, String]]]("headers")
      body = cur.downField("body").focus.flatMap {
        case j if j.isString => j.asString
        case j => Some(j.dropNullValues.spaces2)
      }
      matchingRules <- cur.get[Option[Map[String, MatchingRule]]]("matchingRules")
    } yield InteractionRequest(method, path, query, headers, body, matchingRules)
  }

  implicit val interactionRequestEncoder: Encoder[InteractionRequest] = Encoder.instance { r =>
    val encodedBody: Option[Json] = r.body.map(b => parser.parse(b).toOption.getOrElse(Json.fromString(b)))
    Json.obj(
      "method" -> r.method.asJson,
      "path" -> r.path.asJson,
      "query" -> r.query.asJson,
      "headers" -> r.headers.asJson,
      "body" -> encodedBody.asJson,
      "matchingRules" -> r.matchingRules.asJson
    )
  }

  implicit val interactionResponseDecoder: Decoder[InteractionResponse] = Decoder.instance { cur =>
    for {
      status <- cur.get[Option[Int]]("status")
      headers <- cur.get[Option[Map[String, String]]]("headers")
      body = cur.downField("body").focus.flatMap {
        case j if j.isString => j.asString
        case j => Some(j.dropNullValues.spaces2)
      }
      matchingRules <- cur.get[Option[Map[String, MatchingRule]]]("matchingRules")
    } yield InteractionResponse(status, headers, body, matchingRules)
  }

  implicit val interactionResponseEncoder: Encoder[InteractionResponse] = Encoder.instance { r =>
    val encodedBody: Option[Json] = r.body.map(b => parser.parse(b).toOption.getOrElse(Json.fromString(b)))
    Json.obj(
      "status" -> r.status.asJson,
      "headers" -> r.headers.asJson,
      "body" -> encodedBody.asJson,
      "matchingRules" -> r.matchingRules.asJson
    )
  }

  implicit val interactionDecoder: Decoder[Interaction] = Decoder.instance { cur =>
    for {
      providerState <- cur.get[Option[String]]("providerState")
      provider_state <- cur.get[Option[String]]("provider_state")
      description <- cur.get[String]("description")
      request <- cur.get[InteractionRequest]("request")
      response <- cur.get[InteractionResponse]("response")
    } yield Interaction(providerState.orElse(provider_state), description, request, response)
  }

  implicit val interactionEncoder: Encoder[Interaction] = deriveEncoder

  implicit val linkValueDecoder: Codec[LinkValues] = deriveCodec

  implicit val versionMetaDataDecoder: Codec[VersionMetaData] = deriveCodec

  implicit val pactMetaDataDecoder: Codec[PactMetaData] = deriveCodec

  implicit val pactDecoder: Decoder[Pact] = Decoder.instance { cur =>
    for {
      provider <- cur.get[PactActor]("provider")
      consumer <- cur.get[PactActor]("consumer")
      interactions <- cur.get[List[Interaction]]("interactions")
      _links <- cur.downField("_links").downField("curies").delete.as[Option[Links]]
      metadata <- cur.get[Option[PactMetaData]]("metadata")
    } yield Pact(provider, consumer, interactions, _links, metadata)
  }

  implicit val pactEncoder: Encoder[Pact] = deriveEncoder

  implicit val halIndexDecoder: Decoder[HALIndex] = Decoder.instance { cur =>
    cur.downField("_links").downField("curies").delete.as[Links].map(HALIndex)
  }

  implicit val embeddedPactsForVerificationDecoder: Decoder[EmbeddedPactsForVerification] = deriveDecoder
  implicit val embeddedPactForVerificationDecoder: Decoder[EmbeddedPactForVerification] = deriveDecoder
  implicit val verificationPropertiesDecoder: Decoder[VerificationProperties] = deriveDecoder
  implicit val pactsForVerificationDecoder: Decoder[PactsForVerificationResponse] = deriveDecoder

  implicit val consumerVersionSelectorEncoder: Encoder[ConsumerVersionSelector] = deriveEncoder
  implicit val pactsForVerificationRequestEncoder: Encoder[PactsForVerificationRequest] = deriveEncoder
}

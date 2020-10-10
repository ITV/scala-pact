package com.itv.scalapact.circe09

import com.itv.scalapact.shared.Pact.Links
import com.itv.scalapact.shared._
import io.circe._
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.syntax._

object PactImplicits {

  implicit val pactActorDecoder: Decoder[PactActor] = deriveDecoder
  implicit val pactActorEncoder: Encoder[PactActor] = deriveEncoder

  implicit val matchingRuleDecoder: Decoder[MatchingRule] = deriveDecoder
  implicit val matchingRuleEncoder: Encoder[MatchingRule] = deriveEncoder

  implicit val interactionRequestDecoder: Decoder[InteractionRequest] = Decoder.instance { cur =>
    for {
      method <- cur.get[Option[String]]("method")
      path <- cur.get[Option[String]]("path")
      query <- cur.get[Option[String]]("query")
      headers <- cur.get[Option[Map[String, String]]]("headers")
      body = cur.downField("body").focus.flatMap {
        case j if j.isString => j.asString
        case j => Some(j.pretty(Printer.spaces2.copy(dropNullValues = true)))
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
        case j => Some(j.pretty(Printer.spaces2.copy(dropNullValues = true)))
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

  implicit val linkValueDecoder: Decoder[LinkValues] = deriveDecoder
  implicit val linkValueEncoder: Encoder[LinkValues] = deriveEncoder

  implicit val versionMetaDataDecoder: Decoder[VersionMetaData] = deriveDecoder
  implicit val versionMetaDataEncoder: Encoder[VersionMetaData] = deriveEncoder

  implicit val pactMetaDataDecoder: Decoder[PactMetaData] = deriveDecoder
  implicit val pactMetaDataEncoder: Encoder[PactMetaData] = deriveEncoder

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

  implicit val halIndexDecoder: Decoder[HALIndex] = deriveDecoder

  implicit val embeddedPactsForVerificationDecoder: Decoder[EmbeddedPactsForVerification] = deriveDecoder
  implicit val embeddedPactForVerificationDecoder: Decoder[EmbeddedPactForVerification] = deriveDecoder
  implicit val verificationPropertiesDecoder: Decoder[VerificationProperties] = deriveDecoder
  implicit val pactsForVerificationDecoder: Decoder[PactsForVerificationResponse] = deriveDecoder

  implicit val consumerVersionSelectorEncoder: Encoder[ConsumerVersionSelector] = deriveEncoder
  implicit val pactsForVerificationRequestEncoder: Encoder[PactsForVerificationRequest] = deriveEncoder
}

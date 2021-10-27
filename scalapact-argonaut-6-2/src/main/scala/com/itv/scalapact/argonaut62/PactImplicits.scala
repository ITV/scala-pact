package com.itv.scalapact.argonaut62

import argonaut.Argonaut._
import argonaut._
import com.itv.scalapact.shared.Notice._
import com.itv.scalapact.shared._

import java.time.format.DateTimeFormatter
import scala.util.{Failure, Success, Try}

object PactImplicits {

  implicit lazy val LinkValuesCodecJson: CodecJson[LinkValues] = casecodec4(LinkValues.apply, LinkValues.unapply)(
    "title",
    "name",
    "href",
    "templated"
  )

  implicit lazy val linkDecodeJson: DecodeJson[Link] = {
    implicit val linkValues: DecodeJson[LinkValues] = jdecode4L(LinkValues.apply)(
      "title",
      "name",
      "href",
      "templated"
    )
    val linkList: DecodeJson[LinkList] = DecodeJson.ListDecodeJson[LinkValues].map(LinkList)
    linkValues.widen[Link]() ||| linkList.widen[Link]()
  }

  implicit lazy val linkEncodeJson: EncodeJson[Link] = {
    implicit val linkValues: EncodeJson[LinkValues] = jencode4L { (l: LinkValues) =>
      (l.title, l.name, l.href, l.templated)
    }(
      "title",
      "name",
      "href",
      "templated"
    )
    val linkList: EncodeJson[LinkList] = EncodeJson.ListEncodeJson[LinkValues].contramap(_.links)
    EncodeJson {
      case l: LinkValues =>
        linkValues.encode(l)
      case l: LinkList => linkList.encode(l)
    }
  }

  implicit lazy val scalaPactDecodeJson: DecodeJson[Pact] = DecodeJson[Pact] { cur =>
    for {
      provider     <- cur.get[PactActor]("provider")
      consumer     <- cur.get[PactActor]("consumer")
      interactions <- cur.get[List[Interaction]]("interactions")
      _links       <- cur.downField("_links").as[Option[Links]]
      metadata     <- cur.get[Option[PactMetaData]]("metadata")
    } yield Pact(provider, consumer, interactions, _links, metadata)
  }

  implicit lazy val scalaPactEncodeJson: EncodeJson[Pact] = EncodeJson { p =>
    Json.obj(
      "provider"     -> p.provider.asJson,
      "consumer"     -> p.consumer.asJson,
      "interactions" -> p.interactions.asJson,
      "_links"       -> p._links.asJson,
      "metadata"     -> p.metadata.asJson
    )
  }

  implicit val jvmPactDecoder: DecodeJson[JvmPact] = DecodeJson { cur =>
    for {
      consumer <- cur.get[PactActor]("consumer")
      provider <- cur.get[PactActor]("provider")
      body = cur.focus.nospaces
    } yield JvmPact(consumer, provider, body)
  }

  implicit val jvmPactEncoder: EncodeJson[JvmPact] = EncodeJson { jvmPact =>
    Parse.parse(jvmPact.rawContents) match {
      case Right(value) => value
      case Left(error)  => throw new Exception(s"Generated pact is not valid json: $error")
    }
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

  implicit lazy val InteractionRequestDecodeJson: DecodeJson[InteractionRequest] = DecodeJson[InteractionRequest] {
    cur =>
      val body = cur.downField("body").focus.flatMap {
        case j if j.isString => j.as[String].toOption
        case j               => Some(j.pretty(PrettyParams.spaces2.copy(dropNullKeys = true)))
      }
      for {
        method        <- cur.get[Option[String]]("method")
        path          <- cur.get[Option[String]]("path")
        query         <- cur.get[Option[String]]("query")
        headers       <- cur.get[Option[Map[String, String]]]("headers")
        matchingRules <- cur.get[Option[Map[String, MatchingRule]]]("matchingRules")
      } yield InteractionRequest(method, path, query, headers, body, matchingRules)
  }

  implicit lazy val interactionRequestEncodeJson: EncodeJson[InteractionRequest] = EncodeJson[InteractionRequest] { r =>
    val encodedBody: Option[Json] = r.body.map(b => Parse.parse(b).toOption.getOrElse(Json.jString(b)))
    Json.obj(
      "method"        -> r.method.asJson,
      "path"          -> r.path.asJson,
      "query"         -> r.query.asJson,
      "headers"       -> r.headers.asJson,
      "body"          -> encodedBody.asJson,
      "matchingRules" -> r.matchingRules.asJson
    )
  }

  implicit lazy val InteractionResponseDecodeJson: DecodeJson[InteractionResponse] = DecodeJson[InteractionResponse] {
    cur =>
      val body = cur.downField("body").focus.flatMap {
        case j if j.isString => j.as[String].toOption
        case j               => Some(j.pretty(PrettyParams.spaces2.copy(dropNullKeys = true)))
      }
      for {
        status        <- cur.get[Option[Int]]("status")
        headers       <- cur.get[Option[Map[String, String]]]("headers")
        matchingRules <- cur.get[Option[Map[String, MatchingRule]]]("matchingRules")
      } yield InteractionResponse(status, headers, body, matchingRules)
  }

  implicit lazy val InteractionResponseEncodeJson: EncodeJson[InteractionResponse] = EncodeJson[InteractionResponse] {
    r =>
      val encodedBody: Option[Json] = r.body.map(b => Parse.parse(b).toOption.getOrElse(Json.jString(b)))
      Json.obj(
        "status"        -> r.status.asJson,
        "headers"       -> r.headers.asJson,
        "body"          -> encodedBody.asJson,
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
      providerState  <- cur.get[Option[String]]("providerState")
      provider_state <- cur.get[Option[String]]("provider_state")
      ps = providerState.orElse(provider_state).filterNot(_.isEmpty)
      description <- cur.get[String]("description")
      request     <- cur.get[InteractionRequest]("request")
      response    <- cur.get[InteractionResponse]("response")
    } yield Interaction(ps, description, request, response)
  }

  implicit lazy val InteractionCodecJson: EncodeJson[Interaction] = EncodeJson[Interaction] { i =>
    Json.obj(
      "providerState" -> i.providerState.asJson,
      "description"   -> i.description.asJson,
      "request"       -> i.request.asJson,
      "response"      -> i.response.asJson
    )
  }

  implicit lazy val halIndexDecoder: DecodeJson[HALIndex] =
    DecodeJson[HALIndex](_.downField("_links").as[Links].map(HALIndex))

  implicit val pendingStateNoticeDecoder: DecodeJson[Notice] = DecodeJson[Notice] { cur =>
    lazy val pattern = "after_verification:success_(true|false)_published_(true|false)".r
    cur.get[String](s"text").flatMap { text =>
      cur.get[Option[String]]("when").flatMap {
        case Some("before_verification") => DecodeResult.ok(BeforeVerificationNotice(text))
        case Some(pattern(success, published)) =>
          (for {
            suc <- Try(success.toBoolean)
            pub <- Try(published.toBoolean)
          } yield AfterVerificationNotice(text, suc, pub)) match {
            case Failure(err)   => DecodeResult.fail(err.getMessage, cur.history)
            case Success(value) => DecodeResult.ok(value)
          }
        case _ => DecodeResult.ok(SimpleNotice(text))
      }
    }
  }

  implicit lazy val verificationPropertiesDecoder: DecodeJson[VerificationProperties] =
    DecodeJson[VerificationProperties] { cur =>
      val pending = cur.get[Boolean]("pending").getOr(false)
      cur.get[List[Notice]]("notices").map { ns =>
        VerificationProperties(pending, ns)
      }
    }

  implicit lazy val embeddedPactForVerificationDecodeJson: DecodeJson[PactForVerification] =
    DecodeJson[PactForVerification] { cur =>
      for {
        vp    <- cur.downField("verificationProperties").as[VerificationProperties]
        links <- cur.downField("_links").as[Links]
      } yield PactForVerification(vp, links)
    }

  implicit lazy val pactsForVerificationDecoder: DecodeJson[PactsForVerificationResponse] =
    DecodeJson[PactsForVerificationResponse] { cur =>
      for {
        pacts <- cur.downField("_embedded").downField("pacts").as[List[PactForVerification]]
        links <- cur.downField("_links").as[Links]
      } yield PactsForVerificationResponse(EmbeddedPactsForVerification(pacts), links)
    }

  implicit lazy val consumerVersionSelectorEncodeJson: EncodeJson[ConsumerVersionSelector] =
    EncodeJson[ConsumerVersionSelector] { vs =>
      Json.obj(
        "tag"                -> vs.tag.asJson,
        "fallbackTag"        -> vs.fallbackTag.asJson,
        "consumer"           -> vs.consumer.asJson,
        "latest"             -> vs.latest.asJson,
        "deployedOrReleased" -> vs.deployedOrReleased.asJson,
        "deployed"           -> vs.deployed.asJson,
        "released"           -> vs.released.asJson,
        "environment"        -> vs.environment.asJson
      )
    }

  implicit lazy val pactsForVerificationRequestEncoder: EncodeJson[PactsForVerificationRequest] =
    EncodeJson[PactsForVerificationRequest] { r =>
      val wipPactString = r.includeWipPactsSince.map(DateTimeFormatter.ISO_OFFSET_DATE_TIME.format)
      Json.obj(
        "consumerVersionSelectors" -> r.consumerVersionSelectors.asJson,
        "providerVersionTags"      -> r.providerVersionTags.asJson,
        "includePendingStatus"     -> r.includePendingStatus.asJson,
        "includeWipPactsSince"     -> wipPactString.asJson
      )
    }

}

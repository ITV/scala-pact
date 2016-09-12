package com.itv.scalapactcore.pactspec.util

import argonaut.Argonaut._
import argonaut.{CodecJson, Json}
import com.itv.scalapactcore.{InteractionRequest, InteractionResponse, PactImplicits}

import scala.io.Source
import scalaz.Scalaz._
import scalaz._

import scala.language.implicitConversions

object PactSpecLoader {

  import com.itv.scalapactcore.PactImplicits._

  implicit lazy val RequestSpecCodecJson: CodecJson[RequestSpec] = casecodec4(RequestSpec.apply, RequestSpec.unapply)(
    "match", "comment", "expected", "actual"
  )

  implicit lazy val ResponseSpecCodecJson: CodecJson[ResponseSpec] = casecodec4(ResponseSpec.apply, ResponseSpec.unapply)(
    "match", "comment", "expected", "actual"
  )

  def fromResource(version: String, path: String): String =
    Source.fromURL(getClass.getResource(s"/pact-specification-version-$version/testcases$path")).getLines().mkString("\n")

  def deserializeRequestSpec(json: String): Option[RequestSpec] =
    SpecReader.jsonStringToRequestSpec(json) match {
      case \/-(r) => Option(r)
      case -\/(l) =>
        println(s"Error reading json: $l\n$json")
        None
    }

  def deserializeResponseSpec(json: String): Option[ResponseSpec] =
    SpecReader.jsonStringToResponseSpec(json) match {
      case \/-(r) => Option(r)
      case -\/(l) =>
        println(s"Error reading json: $l\n$json")
        None
    }

}

case class RequestSpec(`match`: Boolean, comment: String, expected: InteractionRequest, actual: InteractionRequest)
case class ResponseSpec(`match`: Boolean, comment: String, expected: InteractionResponse, actual: InteractionResponse)

object SpecReader {

  implicit def tupleToInteractionRequest(pair: (InteractionRequest, Option[String])): InteractionRequest = pair._1.copy(body = pair._2)
  implicit def tupleToInteractionResponse(pair: (InteractionResponse, Option[String])): InteractionResponse = pair._1.copy(body = pair._2)

  type BrokenPact[I] = (Boolean, String, (I, Option[String]), (I, Option[String]))

  def jsonStringToSpec[I]: String => argonaut.DecodeJson[I] => String \/ BrokenPact[I] = json => { implicit decoder =>

    val brokenPact: String \/ BrokenPact[I] = for {
      matches <- JsonBodySpecialCaseHelper.extractMatches(json)
      comment <- JsonBodySpecialCaseHelper.extractComment(json)
      expected <- JsonBodySpecialCaseHelper.extractInteractionRequestOrResponse[I]("expected")(json)(decoder)
      actual <- JsonBodySpecialCaseHelper.extractInteractionRequestOrResponse[I]("actual")(json)(decoder)
    } yield (matches, comment, expected, actual)

    brokenPact
  }

  val jsonStringToRequestSpec: String => String \/ RequestSpec = json =>
    jsonStringToSpec[InteractionRequest](json)(PactImplicits.InteractionRequestCodecJson.Decoder)
      .map { bp => RequestSpec(bp._1, bp._2, bp._3, bp._4) }

  val jsonStringToResponseSpec: String => String \/ ResponseSpec = json =>
    jsonStringToSpec[InteractionResponse](json)(PactImplicits.InteractionResponseCodecJson.Decoder)
      .map { bp => ResponseSpec(bp._1, bp._2, bp._3, bp._4) }

}

object JsonBodySpecialCaseHelper {

  val extractMatches: String => String \/ Boolean = json =>
    json.parse.disjunction.map(j => (j.hcursor --\ "match").focus.flatMap(_.bool).contains(true))

  val extractComment: String => String \/ String = json =>
    json.parse.disjunction.map(j => (j.hcursor --\ "comment").focus.flatMap(_.string).map(_.toString()).getOrElse("<missing comment>"))

  def extractInteractionRequestOrResponse[I]: String => String => argonaut.DecodeJson[I] => String \/ (I, Option[String]) = field => json => { implicit decoder =>
    separateRequestResponseFromBody(field)(json).flatMap(RequestResponseAndBody.unapply) match {
      case Some((Some(requestResponseMinusBody), maybeBody)) =>
        requestResponseMinusBody
          .toString
          .decodeEither[I]
          .disjunction
          .map(i => (i, maybeBody))

      case Some((None, _)) =>
        s"Could not convert request to Json object: $json".left

      case None =>
        s"Problem splitting request from body in: $json".left
    }
  }

  private lazy val separateRequestResponseFromBody: String => String => Option[RequestResponseAndBody] = field => json =>
    json.parseOption.flatMap(j => (j.hcursor --\ field).focus).map { requestField =>

      val minusBody = (requestField.hcursor --\ "body").delete.undo match {
        case ok @ Some(_) => ok
        case None => Some(requestField) // There wasn't a body, but there was still a request.
      }

      val requestBody = (requestField.hcursor --\ "body").focus.flatMap { p =>
        if(p.isString) p.string.map(_.toString) else p.toString.some
      }

      RequestResponseAndBody(minusBody, requestBody)
    }

  final case class RequestResponseAndBody(requestMinusBody: Option[Json], body: Option[String])

}
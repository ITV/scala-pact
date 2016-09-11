package com.itv.scalapactcore.pactspec.util

import argonaut.Argonaut._
import argonaut.{CodecJson, Json}
import com.itv.scalapactcore.{InteractionRequest, InteractionResponse}

import scala.io.Source
import scalaz.Scalaz._
import scalaz._

object PactSpecLoader {

  import com.itv.scalapactcore.PactImplicits._

  def fromResource(version: String, path: String): String =
    Source.fromURL(getClass.getResource(s"/pact-specification-version-$version/testcases$path")).getLines().mkString("\n")

  implicit lazy val RequestSpecCodecJson: CodecJson[RequestSpec] = casecodec4(RequestSpec.apply, RequestSpec.unapply)(
    "match", "comment", "expected", "actual"
  )

  def deserializeRequestSpec(json: String): Option[RequestSpec] =
    SpecReader.jsonStringToRequestSpec(json) match {
      case \/-(r) => Option(r)
      case -\/(l) =>
        println("Error reading json: " + json)
        None
    }

  implicit lazy val ResponseSpecCodecJson: CodecJson[ResponseSpec] = casecodec4(ResponseSpec.apply, ResponseSpec.unapply)(
    "match", "comment", "expected", "actual"
  )

  def deserializeResponseSpec(json: String): Option[ResponseSpec] =
    SpecReader.jsonStringToResponseSpec(json) match {
      case \/-(r) => Option(r)
      case -\/(l) =>
        println("Error reading json: " + json)
        None
    }

}

case class RequestSpec(`match`: Boolean, comment: String, expected: InteractionRequest, actual: InteractionRequest)
case class ResponseSpec(`match`: Boolean, comment: String, expected: InteractionResponse, actual: InteractionResponse)

object SpecReader {

  // TODO: Definitely refactoring to do here, these functions are nearly identical!
  val jsonStringToRequestSpec: String => String \/ RequestSpec = json => {

    val brokenPact: Option[(Boolean, String, InteractionRequest, InteractionRequest)] = for {
      matches <- JsonBodySpecialCaseHelper.extractMatches(json)
      comment <- JsonBodySpecialCaseHelper.extractComment(json)
      expected <- JsonBodySpecialCaseHelper.extractInteractionRequest("expected")(json)
      actual <- JsonBodySpecialCaseHelper.extractInteractionRequest("actual")(json)
    } yield (matches, comment, expected, actual)

    brokenPact.map { bp =>

      RequestSpec(
        `match` = bp._1,
        comment = bp._2,
        expected = bp._3,
        actual = bp._4
      )

    } match {
      case Some(pact) => pact.right
      case None => s"Could not read pact from json: $json".left
    }
  }

  val jsonStringToResponseSpec: String => String \/ ResponseSpec = json => {

    val brokenPact: Option[(Boolean, String, InteractionResponse, InteractionResponse)] = for {
      matches <- JsonBodySpecialCaseHelper.extractMatches(json)
      comment <- JsonBodySpecialCaseHelper.extractComment(json)
      expected <- JsonBodySpecialCaseHelper.extractInteractionResponse("expected")(json)
      actual <- JsonBodySpecialCaseHelper.extractInteractionResponse("actual")(json)
    } yield (matches, comment, expected, actual)

    brokenPact.map { bp =>

      ResponseSpec(
        `match` = bp._1,
        comment = bp._2,
        expected = bp._3,
        actual = bp._4
      )

    } match {
      case Some(pact) => pact.right
      case None => s"Could not read pact from json: $json".left
    }
  }

}

object JsonBodySpecialCaseHelper {

  import com.itv.scalapactcore.PactImplicits._

  val extractMatches: String => String \/ Boolean = json =>
    json.parse.disjunction.map(j => (j.hcursor --\ "match").focus.flatMap(_.bool).contains(true))

  val extractComment: String => String \/ String = json =>
    json.parse.disjunction.map(j => (j.hcursor --\ "comment").focus.flatMap(_.string).map(_.toString()).getOrElse("<missing comment>"))

  val extractInteractionRequest: String => String => String \/ InteractionRequest = field => json =>
    separateRequestResponseFromBody(field)(json).flatMap { pair =>
      pair._1.flatMap(_.toString.decodeOption[InteractionRequest]).map(_.copy(body = pair._2))
    }


  val extractInteractionResponse: String => String => String \/ InteractionResponse = field => json =>
    separateRequestResponseFromBody(field)(json).flatMap { pair =>
      pair._1.flatMap(_.toString.decodeOption[InteractionResponse]).map(_.copy(body = pair._2))
    }

  private lazy val separateRequestResponseFromBody: String => String => Option[(Option[Json], Option[String])] = field => json =>
    json.parseOption.flatMap(j => (j.hcursor --\ field).focus).map { requestField =>

      val minusBody = (requestField.hcursor --\ "body").delete.undo match {
        case ok @ Some(s) => ok
        case None => Some(requestField) // There wasn't a body, but there was still a request.
      }

      val requestBody = (requestField.hcursor --\ "body").focus.flatMap { p =>
        if(p.isString) p.string.map(_.toString) else p.toString.some
      }

      (minusBody, requestBody)
    }

}
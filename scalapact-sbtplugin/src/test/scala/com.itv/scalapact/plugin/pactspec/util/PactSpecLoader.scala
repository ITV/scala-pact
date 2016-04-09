package com.itv.scalapact.plugin.pactspec.util

import argonaut.Argonaut._
import argonaut.{CodecJson, Json}
import com.itv.scalapactcore.{InteractionRequest, InteractionResponse}

import scala.io.Source
import scalaz.Scalaz._
import scalaz._

object PactSpecLoader {

  import com.itv.scalapactcore.PactImplicits._

  def fromResource(path: String): String =
    Source.fromURL(getClass.getResource("/pact-specification-version-2/testcases" + path)).getLines().mkString("\n")

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

  val extractMatches: String => Option[Boolean] = json => {
    val lens = jObjectPL >=> jsonObjectPL("match") >=> jBoolPL
    json.parseOption.flatMap(j => lens.get(j))
  }

  val extractComment: String => Option[String] = json => {
    val lens = jObjectPL >=> jsonObjectPL("comment") >=> jStringPL
    json.parseOption.flatMap(j => lens.get(j))
  }

  val extractInteractionRequest: String => String => Option[InteractionRequest] = field => json => {

    val requestLens = jObjectPL >=> jsonObjectPL(field)
    val bodyStringLens = jObjectPL >=> jsonObjectPL("body") >=> jStringPL
    val bodyJsonLens = jObjectPL >=> jsonObjectPL("body")

    json.parseOption.flatMap(j => requestLens.get(j)).flatMap { requestField =>

      val requestMinusBody: Option[Json] = {
        for {
          bodyField <- requestField.cursor.downField("body")
          updated <- bodyField.delete
        } yield updated.undo
      } match {
        case ok @ Some(s) => ok
        case None => Some(requestField) // There wasn't a body, but there was still a request.
      }

      val requestBody = bodyStringLens.get(requestField).orElse(bodyJsonLens.get(requestField)).map(_.toString)

      requestMinusBody
        .flatMap(_.toString.decodeOption[InteractionRequest])
        .map(_.copy(body = requestBody))
    }
  }

  val extractInteractionResponse: String => String => Option[InteractionResponse] = field => json => {

    val responseLens = jObjectPL >=> jsonObjectPL(field)
    val bodyStringLens = jObjectPL >=> jsonObjectPL(field) >=> jObjectPL >=> jsonObjectPL("body") >=> jStringPL
    val bodyJsonLens = jObjectPL >=> jsonObjectPL(field) >=> jObjectPL >=> jsonObjectPL("body")

    json.parseOption.flatMap(j => responseLens.get(j)).flatMap { responseField =>

      val responseMinusBody: Option[Json] = {
        for {
          bodyField <- responseField.cursor.downField("body")
          updated <- bodyField.delete
        } yield updated.undo
      } match {
        case ok @ Some(s) => ok
        case None => Some(responseField) // There wasn't a body, but there was still a request.
      }

      val responseBody = bodyStringLens.get(responseField).orElse(bodyJsonLens.get(responseField)).map(_.toString)

      responseMinusBody
        .flatMap(_.toString.decodeOption[InteractionResponse])
        .map(_.copy(body = responseBody))
    }
  }


}
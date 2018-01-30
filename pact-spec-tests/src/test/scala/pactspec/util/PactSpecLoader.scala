package pactspec.util

import argonaut.Argonaut._
import argonaut.{CodecJson, Json}
import com.itv.scalapact.shared.{InteractionRequest, InteractionResponse}

import scala.io.Source
import scala.language.implicitConversions
import com.itv.scalapact.shared.RightBiasEither._
import com.itv.scalapact.shared.pact.PactImplicits
import com.itv.scalapact.shared.PactLogger

object PactSpecLoader {

  import PactImplicits._

  implicit lazy val RequestSpecCodecJson: CodecJson[RequestSpec] = casecodec4(RequestSpec.apply, RequestSpec.unapply)(
    "match", "comment", "expected", "actual"
  )

  implicit lazy val ResponseSpecCodecJson: CodecJson[ResponseSpec] = casecodec4(ResponseSpec.apply, ResponseSpec.unapply)(
    "match", "comment", "expected", "actual"
  )

  def fromResource(version: String, path: String): String = {
    //println("Loading spec: " + s"/pact-specification-version-$version/testcases$path")
    Source.fromURL(getClass.getResource(s"/pact-specification-version-$version/testcases$path"))
      .getLines()
      .mkString("\n")
  }

  def deserializeRequestSpec(json: String): Option[RequestSpec] =
    SpecReader.jsonStringToRequestSpec(json) match {
      case Right(r) => Option(r)
      case Left(l) =>
        PactLogger.error(s"Error reading json: $l\n$json")
        None
    }

  def deserializeResponseSpec(json: String): Option[ResponseSpec] =
    SpecReader.jsonStringToResponseSpec(json) match {
      case Right(r) => Option(r)
      case Left(l) =>
        PactLogger.error(s"Error reading json: $l\n$json")
        None
    }

}

case class RequestSpec(`match`: Boolean, comment: String, expected: InteractionRequest, actual: InteractionRequest)
case class ResponseSpec(`match`: Boolean, comment: String, expected: InteractionResponse, actual: InteractionResponse)

object SpecReader {

  implicit def tupleToInteractionRequest(pair: (InteractionRequest, Option[String])): InteractionRequest = pair._1.copy(body = pair._2)
  implicit def tupleToInteractionResponse(pair: (InteractionResponse, Option[String])): InteractionResponse = pair._1.copy(body = pair._2)

  type BrokenPact[I] = (Boolean, String, (I, Option[String]), (I, Option[String]))

  def jsonStringToSpec[I]: String => argonaut.DecodeJson[I] => Either[String, BrokenPact[I]] = json => { implicit decoder =>
    for {
      matches <- JsonBodySpecialCaseHelper.extractMatches(json)
      comment <- JsonBodySpecialCaseHelper.extractComment(json)
      expected <- JsonBodySpecialCaseHelper.extractInteractionRequestOrResponse[I]("expected")(json)(decoder)
      actual <- JsonBodySpecialCaseHelper.extractInteractionRequestOrResponse[I]("actual")(json)(decoder)
    } yield (matches, comment, expected, actual)
  }

  val jsonStringToRequestSpec: String => Either[String, RequestSpec] = json =>
    jsonStringToSpec[InteractionRequest](json)(PactImplicits.InteractionRequestCodecJson.Decoder) match {
      case Right(bp) =>
        Right(RequestSpec(bp._1, bp._2, bp._3, bp._4))

      case Left(s) =>
        Left(s)
    }

  val jsonStringToResponseSpec: String => Either[String, ResponseSpec] = json =>
    jsonStringToSpec[InteractionResponse](json)(PactImplicits.InteractionResponseCodecJson.Decoder) match {
      case Right(bp) =>
        Right(ResponseSpec(bp._1, bp._2, bp._3, bp._4))

      case Left(s) =>
        Left(s)
    }

}

object JsonBodySpecialCaseHelper {

  val extractMatches: String => Either[String, Boolean] = json =>
    json.parse.map(j => (j.hcursor --\ "match").focus.flatMap(_.bool).exists(_ == true)) //Uses exists for 2.10 compt
      .leftMap(e => "Extracting 'match': " + e)

  val extractComment: String => Either[String, String] = json =>
    json.parse.map(j => (j.hcursor --\ "comment").focus.flatMap(_.string).map(_.toString()).getOrElse("<missing comment>"))
      .leftMap(e => "Extracting 'comment': " + e)

  def extractInteractionRequestOrResponse[I]: String => String => argonaut.DecodeJson[I] => Either[String, (I, Option[String])] = field => json => { implicit decoder =>
    separateRequestResponseFromBody(field)(json).flatMap(RequestResponseAndBody.unapply) match {
      case Some((Some(requestResponseMinusBody), maybeBody)) =>
        requestResponseMinusBody
          .toString
          .decodeEither[I]
          .map(i => (i, maybeBody))
          .leftMap(e => "Extracting 'expected or actual': " + e)

      case Some((None, _)) =>
        val msg = s"Could not convert request to Json object: $json"
        PactLogger.error(msg)
        Left(msg)

      case None =>
        val msg = s"Problem splitting request from body in: $json"
        PactLogger.error(msg)
        Left(msg)
    }
  }

  private lazy val separateRequestResponseFromBody: String => String => Option[RequestResponseAndBody] = field => json =>
    json.parseOption.flatMap(j => (j.hcursor --\ field).focus).map { requestField =>

      val minusBody = (requestField.hcursor --\ "body").delete.undo match {
        case ok @ Some(_) => ok
        case None => Some(requestField) // There wasn't a body, but there was still a request.
      }

      val requestBody = (requestField.hcursor --\ "body").focus.flatMap { p =>
        if(p.isString) p.string.map(_.toString) else Option(p.toString)
      }

      RequestResponseAndBody(minusBody, requestBody)
    }

  final case class RequestResponseAndBody(requestMinusBody: Option[Json], body: Option[String])

}
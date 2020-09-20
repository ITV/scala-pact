package com.example.provider

import org.http4s._
import org.http4s.dsl.io._
import org.http4s.circe.jsonEncoderOf
import java.io.File

import org.http4s.implicits._
import cats.effect.IO
import io.circe.Encoder
import org.http4s.headers.`Content-Type`

import scala.io.Source
import scala.util.Random
import org.http4s.util.CaseInsensitiveString

object Provider {

  val service = HttpRoutes.of[IO] {
    case request @ GET -> Root / "results" =>
      val pactHeader = request.headers.get(CaseInsensitiveString("Pact")).map(_.value).getOrElse("")
      Ok(ResultResponse(3, loadPeople)).map(_.putHeaders(Header("Pact", pactHeader)))

    case request @ GET -> Root / "auth_token" =>
      val acceptHeader = request.headers.get(CaseInsensitiveString("Accept")).map(_.value)
      val nameHeader   = request.headers.get(CaseInsensitiveString("Name")).map(_.value)

      (acceptHeader, nameHeader) match {
        case (Some(_), Some(_)) =>
          val token = Random.alphanumeric.take(10).mkString
          Accepted(Token(token))

        case (Some(_), None) =>
          BadRequest("Missing name header")

        case (None, Some(_)) =>
          BadRequest("Missing accept header")

        case (None, None) =>
          BadRequest("Missing accept and name headers")
      }
  }.orNotFound

  def loadPeople: List[String] = {
    val source = Source
      .fromFile(new File("people.txt").toURI)
    val people = source.getLines
      .mkString
      .split(',')
      .toList
    source.close()
    people
  }

}

case class ResultResponse(count: Int, results: List[String])

object ResultResponse {
  implicit val resultsEncoder: Encoder[ResultResponse] =
    Encoder.forProduct2("count", "results")(r => (r.count, r.results))
    implicit val entityEncoder: EntityEncoder[IO, ResultResponse] = jsonEncoderOf[IO, ResultResponse]
}

case class Token(token: String)

object Token {
  implicit val tokenEncoder: Encoder[Token] =
    Encoder.forProduct1("token")(_.token)
  implicit val entityEncoder: EntityEncoder[IO, Token] = jsonEncoderOf[IO, Token]
    .withContentType(`Content-Type`(MediaType.application.json).withCharset(Charset.`UTF-8`))
}

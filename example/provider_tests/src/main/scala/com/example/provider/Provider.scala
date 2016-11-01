package com.example.provider

import org.http4s._
import org.http4s.dsl._
import _root_.argonaut._
import Argonaut._
import org.http4s.argonaut._

import org.http4s.util.CaseInsensitiveString

object Provider {

  import ResultResponseImplicits._
  import TokenResponseImplicits._

  val service: (String => List[String]) => (Int => String) => HttpService = loadPeopleData => genToken => HttpService {
    case GET -> Root / "results" =>
      Ok(ResultResponse(3, loadPeopleData("people.txt")).asJson)

    case request @ GET -> Root / "auth_token" =>

      val acceptHeader = request.headers.get(CaseInsensitiveString("Accept")).map(_.value)
      val nameHeader = request.headers.get(CaseInsensitiveString("Name")).map(_.value)

      (acceptHeader, nameHeader) match {
        case (Some(accept), Some(name)) =>
          val token = genToken(10)
          Accepted(Token(token).asJson)

        case (Some(_), None) =>
          BadRequest("Missing name header")

        case (None, Some(_)) =>
          BadRequest("Missing accept header")

        case (None, None) =>
          BadRequest("Missing accept and name headers")
      }
  }

}

object ResultResponseImplicits {
  implicit lazy val resultsCodec: CodecJson[ResultResponse] =
    casecodec2(ResultResponse.apply, ResultResponse.unapply)("count", "results")
}

case class ResultResponse(count: Int, results: List[String])

object TokenResponseImplicits {
  implicit lazy val tokenCodec: CodecJson[Token] =
    casecodec1(Token.apply, Token.unapply)("token")
}

case class Token(token: String)

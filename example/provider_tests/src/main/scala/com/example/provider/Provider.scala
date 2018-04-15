package com.example.provider

import cats.effect._
import io.circe.generic.auto._
import io.circe.syntax._
import org.http4s._
import org.http4s.circe._
import org.http4s.dsl.io._
import org.http4s.util.CaseInsensitiveString

object Provider {

  val service: (String => List[String]) => (Int => String) => HttpService[IO] = loadPeopleData =>
    genToken =>
      HttpService[IO] {
        case GET -> Root / "results" =>
          Ok(ResultResponse(3, loadPeopleData("people.txt")).asJson)

        case request @ GET -> Root / "auth_token" =>
          val acceptHeader = request.headers.get(CaseInsensitiveString("Accept")).map(_.value)
          val nameHeader   = request.headers.get(CaseInsensitiveString("Name")).map(_.value)

          (acceptHeader, nameHeader) match {
            case (Some(_), Some(_)) =>
              val token = genToken(10)
              Accepted(Token(token).asJson).replaceAllHeaders(Header("Content-Type", "application/json; charset=UTF-8"))

            case (Some(_), None) =>
              BadRequest("Missing name header")

            case (None, Some(_)) =>
              BadRequest("Missing accept header")

            case (None, None) =>
              BadRequest("Missing accept and name headers")
          }
  }

}

case class ResultResponse(count: Int, results: List[String])
case class Token(token: String)

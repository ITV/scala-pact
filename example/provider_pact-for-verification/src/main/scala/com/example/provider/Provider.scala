package com.example.provider

import cats.effect._
import io.circe.generic.auto._
import io.circe.syntax._
import org.http4s._
import org.http4s.circe._
import org.http4s.dsl.io._
import org.http4s.implicits._
import org.typelevel.ci._

object Provider {

  val service: (String => List[String]) => (Int => String) => HttpApp[IO] = loadPeopleData =>
    genToken =>
      HttpRoutes
        .of[IO] {
          case request @ GET -> Root / "results" =>
            val pactHeader = request.headers.get(ci"Pact").map(_.head.value).getOrElse("")

            Ok(ResultResponse(3, loadPeopleData("people.txt")).asJson)
              .map(_.putHeaders(Header.Raw(ci"Pact", pactHeader)))

          case request @ GET -> Root / "auth_token" =>
            val acceptHeader = request.headers.get(ci"Accept").map(_.head.value)
            val nameHeader   = request.headers.get(ci"Name").map(_.head.value)

            (acceptHeader, nameHeader) match {
              case (Some(_), Some(_)) =>
                val token = genToken(10)
                Accepted(Token(token).asJson).map(
                  _.withHeaders(Headers(Header.Raw(ci"Content-Type", "application/json; charset=UTF-8")))
                )

              case (Some(_), None) =>
                BadRequest("Missing name header")

              case (None, Some(_)) =>
                BadRequest("Missing accept header")

              case (None, None) =>
                BadRequest("Missing accept and name headers")
            }
        }
        .orNotFound

}

case class ResultResponse(count: Int, results: List[String])
case class Token(token: String)

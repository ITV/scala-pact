package com.example.provider

import org.http4s._
import org.http4s.dsl._

import _root_.argonaut._, Argonaut._
import org.http4s.argonaut._

object Provider {

  import ResultResponseImplicits._

  val service = HttpService {
    case GET -> Root / "results" =>
      Ok(
        ResultResponse(3, List("Harry", "Fred", "Bob")).asJson
      )
  }
}

object ResultResponseImplicits {

  implicit lazy val resultsCodec: CodecJson[ResultResponse] = casecodec2(ResultResponse.apply, ResultResponse.unapply)("count", "results")

}

case class ResultResponse(count: Int, results: List[String])
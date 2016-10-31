package com.example.provider

import org.http4s._
import org.http4s.dsl._
import _root_.argonaut._
import Argonaut._
import org.http4s.argonaut._
import java.io.File

import scala.io.Source

object Provider {

  import ResultResponseImplicits._

  val service = HttpService {
    case GET -> Root / "results" =>
      Ok(
        ResultResponse(3, loadPeople).asJson
      )
  }

  def loadPeople: List[String] =
    Source.fromFile(new File("people.txt").toURI)
      .getLines
      .mkString
      .split(',')
      .toList
  
}

object ResultResponseImplicits {

  implicit lazy val resultsCodec: CodecJson[ResultResponse] = casecodec2(ResultResponse.apply, ResultResponse.unapply)("count", "results")

}

case class ResultResponse(count: Int, results: List[String])
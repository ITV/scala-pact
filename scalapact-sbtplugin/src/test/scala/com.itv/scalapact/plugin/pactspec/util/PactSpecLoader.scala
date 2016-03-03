package com.itv.scalapact.plugin.pactspec.util

import argonaut.Argonaut._
import argonaut.CodecJson
import com.itv.scalapactcore.{InteractionRequest, InteractionResponse}

import scala.io.Source

object PactSpecLoader {

  import com.itv.scalapactcore.PactImplicits._

  def fromResource(path: String): String =
    Source.fromURL(getClass.getResource("/pact-specification-version-2/testcases" + path)).getLines().mkString("\n")

  implicit lazy val RequestSpecCodecJson: CodecJson[RequestSpec] = casecodec4(RequestSpec.apply, RequestSpec.unapply)(
    "match", "comment", "expected", "actual"
  )

  def deserializeRequestSpec(json: String): Option[RequestSpec] =
    json.decodeOption[RequestSpec]

  implicit lazy val ResponseSpecCodecJson: CodecJson[ResponseSpec] = casecodec4(ResponseSpec.apply, ResponseSpec.unapply)(
    "match", "comment", "expected", "actual"
  )

  def deserializeResponseSpec(json: String): Option[ResponseSpec] =
    json.decodeOption[ResponseSpec]

}

case class RequestSpec(`match`: Boolean, comment: String, expected: InteractionRequest, actual: InteractionRequest)
case class ResponseSpec(`match`: Boolean, comment: String, expected: InteractionResponse, actual: InteractionResponse)

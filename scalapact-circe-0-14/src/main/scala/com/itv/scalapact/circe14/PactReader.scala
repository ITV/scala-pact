package com.itv.scalapact.circe14

import com.itv.scalapact.shared._
import com.itv.scalapact.shared.json.IPactReader
import com.itv.scalapact.shared.matchir.IrNode
import io.circe.Decoder
import io.circe.parser._

class PactReader extends IPactReader {
  import PactImplicits._

  def fromJSON(jsonString: String): Option[IrNode] =
    JsonConversionFunctions.fromJSON(jsonString)

  def jsonStringToScalaPact(json: String): Either[String, Pact] =
    readJson[Pact](json, "scala-pact pact")

  def jsonStringToJvmPact(json: String): Either[String, JvmPact] = readJson[JvmPact](json, "pact-jvm pact")

  def jsonStringToPactsForVerification(json: String): Either[String, PactsForVerificationResponse] =
    readJson[PactsForVerificationResponse](json, "pacts for verification")

  def jsonStringToHALIndex(json: String): Either[String, HALIndex] =
    readJson[HALIndex](json, "HAL index")

  private def readJson[A: Decoder](json: String, dataType: String): Either[String, A] =
    parse(json).flatMap(_.as[A]) match {
      case Right(a) => Right(a)
      case Left(_)  => Left(s"Could not read $dataType from json: $json")
    }
}

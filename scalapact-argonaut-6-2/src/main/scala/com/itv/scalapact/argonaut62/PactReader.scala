package com.itv.scalapact.argonaut62

import argonaut._
import com.itv.scalapact.shared._
import com.itv.scalapact.shared.json.IPactReader
import com.itv.scalapact.shared.matchir.IrNode

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

  private def readJson[A: DecodeJson](json: String, dataType: String): Either[String, A] =
    Parse.parse(json).toOption.flatMap(_.as[A].toOption) match {
      case Some(a) => Right(a)
      case None    => Left(s"Could not read $dataType from json: $json")
    }
}

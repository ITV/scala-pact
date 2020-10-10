package com.itv.scalapact.circe11

import com.itv.scalapact.shared._
import com.itv.scalapact.shared.matchir.IrNode
import com.itv.scalapact.shared.typeclasses.IPactReader
import io.circe.parser._

class PactReader extends IPactReader {
  import PactImplicits._

  def fromJSON(jsonString: String): Option[IrNode] =
    JsonConversionFunctions.fromJSON(jsonString)

  def jsonStringToPact(json: String): Either[String, Pact] =
    parse(json).flatMap(_.as[Pact]) match {
      case Right(p) => Right(p)
      case Left(_) => Left(s"Could not read pact from json: $json")
    }
}

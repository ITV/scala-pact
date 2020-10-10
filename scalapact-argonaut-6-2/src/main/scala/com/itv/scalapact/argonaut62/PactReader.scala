package com.itv.scalapact.argonaut62

import argonaut._
import com.itv.scalapact.shared._
import com.itv.scalapact.shared.matchir.IrNode
import com.itv.scalapact.shared.typeclasses.IPactReader

class PactReader extends IPactReader {
  import PactImplicits._

  def fromJSON(jsonString: String): Option[IrNode] =
    JsonConversionFunctions.fromJSON(jsonString)

  def jsonStringToPact(json: String): Either[String, Pact] =
    Parse.parse(json).toOption.flatMap(_.as[Pact].toOption) match {
      case Some(p) => Right(p)
      case None => Left(s"Could not read pact from json: $json")
    }

}

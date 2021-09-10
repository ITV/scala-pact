package com.itv.scalapact.circe14

import com.itv.scalapact.shared.{JvmPact, Pact}
import com.itv.scalapact.shared.json.{ContractDeserializer, IPactReader, IPactWriter}
import io.circe.parser.parse

trait JsonInstances {
  import PactImplicits._

  implicit val pactReaderInstance: IPactReader = new PactReader

  implicit val pactWriterInstance: IPactWriter = new PactWriter

  implicit val pactDeserializer: ContractDeserializer[Pact] = (jsonString: String) =>
    parse(jsonString).flatMap(_.as[Pact]) match {
      case Right(a) => Right(a)
      case Left(_)  => Left(s"Could not read scala-pact pact from json: $jsonString")
    }

  implicit val jvmPactDeserializer: ContractDeserializer[JvmPact] = (jsonString: String) =>
    parse(jsonString).flatMap(_.as[JvmPact]) match {
      case Right(a) => Right(a)
      case Left(_)  => Left(s"Could not read jvm-pact pact from json: $jsonString")
    }
}

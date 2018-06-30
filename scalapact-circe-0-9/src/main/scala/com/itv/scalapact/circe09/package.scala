package com.itv.scalapact

import com.itv.scalapact.shared.{JsonRepresentation, MessageContentType}
import com.itv.scalapact.shared.typeclasses.{IMessageFormat, IPactReader, IPactWriter, MessageFormatError}
import io.circe.Json
import io.circe.parser.parse
import cats.syntax.either._

package object circe09 {

  object `application/json` extends MessageContentType {
    override def renderString = "application/json"
    override def jsonRepresentation = JsonRepresentation.AsObject
  }

  implicit object JsonMessageFormat extends IMessageFormat[Json] {
    override def contentType: MessageContentType = `application/json`
    override def encode(json: Json): String = json.toString()
    override def decode(s: String): Either[MessageFormatError, Json] =
      parse(s).leftMap(err => MessageFormatError(err.getMessage()))
  }

  implicit val pactReaderInstance: IPactReader =
    new PactReader

  implicit val pactWriterInstance: IPactWriter =
    new PactWriter
}

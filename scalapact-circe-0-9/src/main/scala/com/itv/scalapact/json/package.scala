package com.itv.scalapact

import com.itv.scalapact.circe09.{PactReader, PactWriter}
import com.itv.scalapact.shared.MessageContentType
import com.itv.scalapact.shared.MessageContentType.ApplicationJson
import com.itv.scalapact.shared.typeclasses.{IMessageFormat, IPactReader, IPactWriter, MessageFormatError}
import io.circe.Json
import io.circe.parser.parse

package object json {
  implicit val pactReaderInstance: IPactReader =
    new PactReader

  implicit val pactWriterInstance: IPactWriter =
    new PactWriter

  implicit val jsonMessageFormatInstance: IMessageFormat[Json] = new IMessageFormat[Json] {
    override def contentType: MessageContentType = ApplicationJson

    override def encode(t: Json): String = t.noSpaces

    override def decode(s: String): Either[MessageFormatError, Json] =
      parse(s).fold(m => Left(MessageFormatError(m.message)), Right(_))
  }
}

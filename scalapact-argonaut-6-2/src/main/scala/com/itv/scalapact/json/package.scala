package com.itv.scalapact

import argonaut.{Json, Parse}
import com.itv.scalapact.argonaut62.{PactReader, PactWriter}
import com.itv.scalapact.shared.MessageContentType
import com.itv.scalapact.shared.MessageContentType.ApplicationJson
import com.itv.scalapact.shared.typeclasses._

package object json {
  implicit val pactReaderInstance: IPactReader =
    new PactReader

  implicit val pactWriterInstance: IPactWriter =
    new PactWriter

  implicit val jsonMessageFormatInstance: IMessageFormat[Json] = new IMessageFormat[Json] {
    override def contentType: MessageContentType = ApplicationJson

    override def encode(t: Json): String = t.nospaces

    override def decode(s: String): Either[MessageFormatError, Json] =
      Parse.parse(s).fold(m => Left(MessageFormatError(m)), Right(_))
  }
  implicit val inferTypeInstance: IInferTypes[Json] = new IInferTypes[Json] {
    override protected def inferFrom(t: Json): Map[String, String] = Map.empty
  }

}

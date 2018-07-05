package com.itv.scalapact

import argonaut.{Json, Parse}
import com.itv.scalapact.argonaut62.{PactReader, PactWriter}
import com.itv.scalapact.shared.MessageContentType
import com.itv.scalapact.shared.MessageContentType.ApplicationJson
import com.itv.scalapact.shared.typeclasses.{IMessageFormat, IPactReader, IPactWriter, MessageFormatError}

package object json {
  implicit val pactReaderInstance: IPactReader =
    new PactReader

  implicit val pactWriterInstance: IPactWriter =
    new PactWriter

  implicit val jsonMessageFormatInstance = new IMessageFormat[Json] {
    override def contentType: MessageContentType = ApplicationJson

    override def encode(t: Json): String = t.nospaces

    override def decode(s: String): Either[MessageFormatError, Json] =
      Parse.parse(s).fold(m => Left(MessageFormatError(m)), Right(_))
  }

}

package com.itv.scalapact

import org.scalatest.FlatSpec
import com.itv.scalapact.{ScalaPactForger, ScalaPactMockConfig}
import com.itv.scalapact.ScalaPactForger.{forgePact, interaction, message}
import com.itv.scalapact.shared.MessageContentType.ApplicationJson
import com.itv.scalapact.shared.{Message, MessageContentType}
import com.itv.scalapact.shared.typeclasses.{IMessageFormat, MessageFormatError}
import org.scalatest.Matchers._

class ScalaPactForgerTest extends FlatSpec {
  it should "create a specification of the message" in {
     val expectedMessage = ApplicationJson.renderString

    forgePact
      .between("Consumer")
      .and("Provider")
      .addMessage(
        message
          .description("")
          //TODO: .expectsToReceive("something")
          .withMeta(Map.empty)
          .withContent(expectedMessage)(new IMessageFormat[String] {
            override def contentType: MessageContentType = ApplicationJson
            override def encode(t: String): String = contentType.renderString
            override def decode(s: String): Either[MessageFormatError, String] = Right(contentType.renderString)
          })
      ).runMessageTests { actualMessage: Message =>
      actualMessage.content should ===(expectedMessage)
    }
  }
}

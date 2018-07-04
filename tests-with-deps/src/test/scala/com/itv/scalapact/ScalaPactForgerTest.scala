package com.itv.scalapact

import java.util.concurrent.atomic.AtomicReference

import argonaut.Json
import argonaut._
import Argonaut._
import com.itv.scalapact.ScalaPactForger.{
  ScalaPactInteractionFinal,
  ScalaPactOptions,
  forgePact,
  interaction,
  message,
  messageSpec
}
import com.itv.scalapact.ScalaPactVerify.ScalaPactVerifyFailed
import com.itv.scalapact.shared.MessageContentType.ApplicationJson
import com.itv.scalapact.shared.typeclasses.{IMessageFormat, MessageFormatError}
import com.itv.scalapact.shared.{Message, MessageContentType}
import org.scalactic.TypeCheckedTripleEquals
import org.scalatest.{EitherValues, FlatSpec, OptionValues}
import org.scalatest.Matchers._

class ScalaPactForgerTest extends FlatSpec with OptionValues with EitherValues with TypeCheckedTripleEquals {

  import com.itv.scalapact.json._
  implicit val defaultOptions = ScalaPactOptions(writePactFiles = true, outputPath = "/tmp")
  import messageSpec._

  val noMetadata = Map.empty[String, String]

  //TODO Add default one in each json module
  implicit val jsonMessageFormat = new IMessageFormat[Json] {
    override def contentType: MessageContentType = ApplicationJson

    override def encode(t: Json): String = t.nospaces

    override def decode(s: String): Either[MessageFormatError, Json] =
      Parse.parse(s).fold(m => Left(MessageFormatError(m)), Right(_))
  }

  val expectedMessage = Json.obj("key1" -> jNumber(1), "key2" -> jString("foo"))

  val specWithOneMessage = forgePact
    .between("Consumer")
    .and("Provider")
    .addMessage(
      message
        .description("description")
        .withProviderState("whatever")
        .withMeta(noMetadata)
        .withContent(expectedMessage)
    )

  val specWithMultipleMessages = specWithOneMessage
    .addMessage(
      message
        .description("description2")
        .withProviderState("whatever")
        .withMeta(noMetadata)
        .withContent(expectedMessage)
    )

  it should "create a specification of the message" in {
    val writer = StubContractWriter()

    val actualMessage = specWithOneMessage.runMessageTests[Message] {
      _.consume("description") { message =>
        message.contentType should ===(ApplicationJson)

        message.meta should ===(noMetadata)
        message.content should ===(expectedMessage.nospaces)
        message
      }
    }(writer, implicitly, implicitly)
    writer.messages should ===(actualMessage)
  }

  it should "create a specification for multiple messages" in {
    val writer = StubContractWriter()

    specWithMultipleMessages
      .runMessageTests[Message](stub => {
        val newStub = stub
          .consume("description") { message =>
            message.contentType should ===(ApplicationJson)
            message.meta should ===(noMetadata)
            message.content should ===(expectedMessage.nospaces)
            message
          }
          .consume("description2") { message =>
            message.contentType should ===(ApplicationJson)
            message.meta should ===(noMetadata)
            message.content should ===(expectedMessage.nospaces)
            message
          }

        writer.messages.map(Right(_)) should contain theSameElementsAs newStub.currentResult
        newStub
      })(writer, implicitly, implicitly)

  }

  it should "create a specification for multiple messages and interactions" in {
    val writer = StubContractWriter()

    specWithMultipleMessages
      .addInteraction(
        interaction
          .description("Some interaction")
          .given("Some state")
          .willRespondWith(200)
      )
      .runMessageTests[Unit](_.consume("description") { _ =>
        ()
      })(writer, implicitly, implicitly)
    writer.interactions should have size 1
    writer.messages should have size 2
  }

  it should "fail when the description does not exist" in {

    a[ScalaPactVerifyFailed] should be thrownBy {
      specWithOneMessage.runMessageTests[Unit] {
        _.consume("description1") { message =>
          }
      }
    }
  }

  it should "fail when the description does not exist with multiple messages" in {

    a[ScalaPactVerifyFailed] should be thrownBy {
      specWithMultipleMessages.runMessageTests[Any] { stub =>
        stub
          .consume("description1") { message =>
            }
          .consume("description2") { message =>
            }
      }
    }
  }

  it should "succeed if the published message is in the right format" in {
    specWithOneMessage.runMessageTests[Any] {
      _.publish("description", expectedMessage)
    }
  }

  it should "succeed if the published message is in the right format when we send metadata it is not required" in {
    specWithOneMessage.runMessageTests[Any] {
      _.publish("description", expectedMessage, Message.Metadata("foo" -> "foo1"))
    }
  }

  it should "fail if the published message is not in the right shape" in {
    a[ScalaPactVerifyFailed] should be thrownBy {
      specWithOneMessage.runMessageTests[Any] {
        _.publish("description", Json.jNull)
      }
    }
  }

  it should "fail if the published message metadata doesn't match the expected metadata" in {
    val expectedContent = Json.obj("key" -> jString("value"))
    val m = message
      .description("a message with the wrong metadata")
      .withMeta(Message.Metadata("foo" -> "bar"))
      .withContent(expectedContent)

    a[ScalaPactVerifyFailed] should be thrownBy {
      forgePact
        .between("Consumer")
        .and("Provider")
        .addMessage(m)
        .runMessageTests[Any] {
          _.publish(m.description, expectedContent, Message.Metadata.empty)
        }
    }
  }

  it should "fail if the published message when the fields are the same but not the same type" in {
    a[ScalaPactVerifyFailed] should be thrownBy {
      specWithOneMessage.runMessageTests[Any] {
        _.publish("description", Json.obj("key1" -> jString("1"), "key2" -> jString("foo3")))
      }
    }
  }

  case class StubContractWriter(
      actualPact: AtomicReference[Option[ScalaPactForger.ScalaPactDescriptionFinal]] = new AtomicReference(None)
  ) extends IContractWriter {

    def messages: List[Message] = actualPact.get().map(_.messages).toList.flatten

    def interactions: List[ScalaPactInteractionFinal] = actualPact.get().map(_.interactions).toList.flatten

    override def writeContract(scalaPactDescriptionFinal: ScalaPactForger.ScalaPactDescriptionFinal): Unit =
      actualPact.set(Some(scalaPactDescriptionFinal))
  }

}

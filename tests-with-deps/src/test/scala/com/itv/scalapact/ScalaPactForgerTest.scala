package com.itv.scalapact

import java.util.concurrent.atomic.AtomicReference

import com.itv.scalapact.ScalaPactForger.{
  IContractWriter,
  ScalaPactInteractionFinal,
  ScalaPactOptions,
  forgePact,
  interaction,
  message
}
import com.itv.scalapact.ScalaPactVerify.ScalaPactVerifyFailed
import com.itv.scalapact.argonaut62.PactWriter
import com.itv.scalapact.shared.MessageContentType.ApplicationJson
import com.itv.scalapact.shared.typeclasses.{IMessageFormat, MessageFormatError}
import com.itv.scalapact.shared.{Message, MessageContentType}
import org.scalactic.TypeCheckedTripleEquals
import org.scalatest.{FlatSpec, OptionValues}
import org.scalatest.Matchers._

class ScalaPactForgerTest extends FlatSpec with OptionValues with TypeCheckedTripleEquals {

  implicit val defaultWriter = new PactWriter

  val noMetadata = Map.empty[String, String]

  implicit val defaultOptions =
    ScalaPactOptions(writePactFiles = true, outputPath = "/tmp")

  implicit val defaultWriterContract = IContractWriter()

  val stringMessageFormat = new IMessageFormat[String] {
    override def contentType: MessageContentType = ApplicationJson
    override def encode(t: String): String = contentType.renderString
    override def decode(s: String): Either[MessageFormatError, String] = Right(contentType.renderString)
  }

  val expectedMessage = ApplicationJson.renderString

  val specWithOneMessage = forgePact
    .between("Consumer")
    .and("Provider")
    .addMessage(
      message
        .description("description")
        .withProviderState("whatever")
        .withMeta(noMetadata)
        .withContent(expectedMessage)(stringMessageFormat)
    )

  val specWithMultipleMessages = specWithOneMessage
    .addMessage(
      message
        .description("description2")
        .withProviderState("whatever")
        .withMeta(noMetadata)
        .withContent(expectedMessage)(stringMessageFormat)
    )

  it should "create a specification of the message" in {
    val writer = StubContractWriter()

    val actualMessage = specWithOneMessage.runMessageTests[Message] {
      _.consume("description") { message =>
        message.contentType should ===(ApplicationJson)

        message.meta should ===(noMetadata)
        message.content should ===(expectedMessage)
        message
      }
    }(writer)
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
            message.content should ===(expectedMessage)
            message
          }
          .consume("description2") { message =>
            message.contentType should ===(ApplicationJson)
            message.meta should ===(noMetadata)
            message.content should ===(expectedMessage)
            message
          }

        writer.messages.map(Right(_)) should contain theSameElementsAs newStub.currentResult
        newStub
      })(writer)

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
      })(writer)
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

  it should "fail if the published message is not in the right format" in {
    a[ScalaPactVerifyFailed] should be thrownBy {
      specWithOneMessage.runMessageTests[Any] {
        _.publish("description", "")
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

package com.itv.scalapact

import argonaut.Json
import argonaut._
import Argonaut._
import com.itv.scalapact.ScalaPactForger.{ScalaPactOptions, forgePact, interaction, message, messageSpec}
import com.itv.scalapact.ScalaPactVerify.ScalaPactVerifyFailed
import com.itv.scalapact.shared.MessageContentType.ApplicationJson
import com.itv.scalapact.shared.{MatchingRule, Message}
import org.scalactic.TypeCheckedTripleEquals
import org.scalatest.{EitherValues, FlatSpec, OptionValues}
import org.scalatest.Matchers._

class ScalaPactForgerSpec extends FlatSpec with OptionValues with EitherValues with TypeCheckedTripleEquals {

  import com.itv.scalapact.json._
  implicit val defaultOptions = ScalaPactOptions(writePactFiles = true, outputPath = "/tmp")
  import messageSpec._

  val firstExpectedMessage = Json.obj("key1" -> jNumber(1), "key2" -> jString("444"))

  val specWithOneMessage = forgePact
    .between("Consumer")
    .and("Provider")
    .addMessage(
      message
        .description("description")
        .withProviderState("whatever")
        .withMeta(Message.Metadata.empty)
        .withRegex(".key2", "\\d+")
        .withContent(firstExpectedMessage)
    )

  val secondExpectedMessage  = Json.obj("key5"       -> jNumber(1), "key6" -> jString("foo"))
  val secondExpectedMetadata = Message.Metadata("id" -> "333")

  val specWithMultipleMessages = specWithOneMessage
    .addMessage(
      message
        .description("description2")
        .withProviderState("whatever")
        .withMeta(secondExpectedMetadata)
        .withContent(secondExpectedMessage)
    )

  it should "create a specification of the message with the associated matching rules" in {
    val writer = StubContractWriter()

    val actualMessage = specWithOneMessage
      .runMessageTests[Message] {
        _.consume("description") { message =>
          message.contentType should ===(ApplicationJson)
          message.matchingRules should ===(Map(".key2" -> MatchingRule(Some("regex"), Some("\\d+"), None)))
          message.metaData should ===(Message.Metadata.empty)
          toJson(message.contents) should ===(firstExpectedMessage)
          message
        }
      }(writer, implicitly)
    writer.messages should ===(actualMessage)
  }

  it should "create a specification for multiple messages" in {
    val writer = StubContractWriter()

    specWithMultipleMessages
      .runMessageTests[Message](stub => {
        val newStub = stub
          .consume("description") { message =>
            message.contentType should ===(ApplicationJson)
            message.metaData should ===(Message.Metadata.empty)
            toJson(message.contents) should ===(firstExpectedMessage)
            message
          }
          .consume("description2") { message =>
            message.contentType should ===(ApplicationJson)
            message.metaData should ===(secondExpectedMetadata)
            toJson(message.contents) should ===(secondExpectedMessage)
            message
          }

        writer.messages should contain theSameElementsAs newStub.results
        newStub
      })(writer, implicitly)

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
      })(writer, implicitly)
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
      _.publish("description", firstExpectedMessage)
    }
  }

  it should "succeed in strict mode if the published message is in the right format" in {
    specWithOneMessage.runStrictMessageTests[Any] {
      _.publish("description", firstExpectedMessage)
    }
  }

  it should "succeed if the published message is in the right format and different content" in {
    specWithOneMessage.runMessageTests[Any] {
      _.publish("description", Json.obj("key1" -> jNumber(1), "key2" -> jString("123"), "foo" -> jString("bar")))
    }
  }

  it should "fail in strict mode if the published message is in the right format but has some extra field" in {
    a[ScalaPactVerifyFailed] should be thrownBy {
      specWithOneMessage.runStrictMessageTests[Any] {
        _.publish("description",
                  Json.obj("key1" -> jNumber(1), "key2" -> jString("444"), "foo" -> jString("unexpected key")))
      }
    }

  }

  it should "fail if the published message is in the right format however it breaks the rules" in {
    a[ScalaPactVerifyFailed] should be thrownBy {
      specWithOneMessage.runMessageTests[Any] {
        _.publish("description", Json.obj("key1" -> jNumber(1), "key2" -> jString("foo")))
      }
    }
  }

  it should "succeed if the published message is in the right format when we send metadata it is not required" in {
    specWithOneMessage.runMessageTests[Any] {
      _.publish("description", firstExpectedMessage, Message.Metadata("foo" -> "foo1"))
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

  def toJson(value: String) = Parse.parse(value).right.value

}

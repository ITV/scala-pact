package com.itv.scalapact

import argonaut.Json
import argonaut._
import Argonaut._
import com.itv.scalapact.ScalaPactForger.{ScalaPactOptions, forgePact, message, messageSpec}
import com.itv.scalapact.shared.typeclasses.IInferTypes
import com.itv.scalapact.shared.{MatchingRule, Message, ProviderState}
import org.scalactic.TypeCheckedTripleEquals
import org.scalatest.{EitherValues, FlatSpec, OptionValues}
import org.scalatest.Matchers._

class TypeInfererSpec extends FlatSpec with OptionValues with EitherValues with TypeCheckedTripleEquals {
  import json.{inferTypeInstance => _, _}
  import messageSpec._
  implicit val defaultOptions = ScalaPactOptions(writePactFiles = true, outputPath = "/tmp")

  it should "add no matching rules if the typer inferer doesn't infer them" in {
    import IInferTypes.noneInferTypeInstance
    val writer = StubContractWriter()

    matchingRulesFrom(baseSpec(
                        baseMessage
                          .withContent(Json.obj("key1" -> jNumber(1), "key2" -> jString("444")))
                      ),
                      writer) shouldBe List(Map.empty)
  }

  it should "add the infered matching rules to the message" in {
    implicit val typeInferer: IInferTypes[Json] = inferTypesFrom(Map("$.body.key1" -> "int"))

    val writer = StubContractWriter()

    matchingRulesFrom(baseSpec(
                        baseMessage
                          .withContent(Json.obj("key1" -> jNumber(1), "key2" -> jString("444")))
                      ),
                      writer) should ===(
      List(
        Map(
          "body" ->
            Map(
              "$.key1" -> Message.Matchers.from(MatchingRule(Some("int"), None, None))
            )
        )
      )
    )
  }

  it should "merge the inferred matching rules to the message" in {
    implicit val typeInferer: IInferTypes[Json] = inferTypesFrom(Map("$.body.key2" -> "integer"))

    val writer = StubContractWriter()

    matchingRulesFrom(baseSpec(
                        baseMessage
                          .withRegexMatchingRule("$.body.key2", "\\d+")
                          .withContent(Json.obj("key1" -> jNumber(1), "key2" -> jString("444")))
                      ),
                      writer) should ===(
      List(
        Map(
          "body" -> Map(
            "$.key2" -> Message.Matchers.from(MatchingRule(Some("regex"), Some("\\d+"), None),
                                              MatchingRule(Some("integer"), None, None))
          )
        )
      )
    )
  }

  private def inferTypesFrom(stringToString: Map[JsonField, JsonField]): IInferTypes[Json] =
    new IInferTypes[Json] {
      override protected def inferFrom(t: Json): Map[String, String] = stringToString
    }

  def toJson(value: String) = Parse.parse(value).right.value

  private def baseSpec(message: Message): ScalaPactForger.forgePact.ScalaPactDescription =
    forgePact
      .between("Consumer")
      .and("Provider")
      .addMessage(
        message
      )

  private def baseMessage: ScalaPactForger.PartialScalaPactMessage =
    message
      .description("description")
      .withProviderState(ProviderState("whatever", Map.empty))
      .withMeta(Message.Metadata.empty)

  private def matchingRulesFrom(
      description: ScalaPactForger.forgePact.ScalaPactDescription,
      writer: IContractWriter
  ): List[Message.MatchingRules] =
    description.runMessageTests[Message.MatchingRules] {
      _.consume("description") { message =>
        message.matchingRules
      }
    }(writer, implicitly)

}

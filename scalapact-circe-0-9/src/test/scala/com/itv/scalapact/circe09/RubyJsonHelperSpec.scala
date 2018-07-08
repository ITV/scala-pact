package com.itv.scalapact.circe09

import com.itv.scalapact.shared._
import org.scalactic.TypeCheckedTripleEquals
import org.scalatest.{EitherValues, FunSpec, Matchers}
import io.circe.parser.parse
import Matchers._

class RubyJsonHelperSpec extends FunSpec with EitherValues with TypeCheckedTripleEquals {

  describe("Handling ruby json") {

    it("should be able to extract the provider") {

      JsonBodySpecialCaseHelper
        .extractPactActor("provider")(parse(PactFileExamples.simpleAsString).right.value)
        .right
        .value should ===(
        PactActor("provider")
      )

    }

    it("should be able to extract the consumer") {

      JsonBodySpecialCaseHelper
        .extractPactActor("consumer")(parse(PactFileExamples.simpleAsString).right.value)
        .right
        .value should ===(
        PactActor("consumer")
      )

    }

    it("should be able to extract a list of interactions paired with their bodies") {

      val interaction1 = Interaction(
        provider_state = None,
        providerState = Option("a simple state"),
        description = "a simple request",
        request = InteractionRequest(
          method = Option("GET"),
          path = Option("/fetch-json"),
          query = Option("fish=chips"),
          headers = Option(Map("Content-Type" -> "text/plain")),
          body = None,
          matchingRules = Option(
            Map(
              "$.headers.Accept"         -> MatchingRule(`match` = Option("regex"), regex = Option("\\w+"), min = None),
              "$.headers.Content-Length" -> MatchingRule(`match` = Option("type"), regex = None, min = None)
            )
          )
        ),
        response = InteractionResponse(
          status = Option(200),
          headers = Option(Map("Content-Type" -> "application/json")),
          body = None,
          matchingRules = Option(
            Map(
              "$.headers.Accept"         -> MatchingRule(`match` = Option("regex"), regex = Option("\\w+"), min = None),
              "$.headers.Content-Length" -> MatchingRule(`match` = Option("type"), regex = None, min = None)
            )
          )
        )
      )
      val interaction1RequestBody  = Option("fish")
      val interaction1ResponseBody = Option("""{
                                              |  "fish" : [
                                              |    "cod",
                                              |    "haddock",
                                              |    "flying"
                                              |  ]
                                              |}""".stripMargin)

      val interaction2 = Interaction(
        provider_state = None,
        providerState = Option("a simple state 2"),
        description = "a simple request 2",
        request = InteractionRequest(
          method = Option("GET"),
          path = Option("/fetch-json2"),
          query = None,
          headers = Option(Map("Content-Type" -> "text/plain")),
          body = None,
          matchingRules = None
        ),
        response = InteractionResponse(
          status = Option(200),
          headers = Option(Map("Content-Type" -> "application/json")),
          body = None,
          matchingRules = None
        )
      )
      val interaction2RequestBody  = Option("fish")
      val interaction2ResponseBody = Option("""{
                                              |  "chips" : true,
                                              |  "fish" : [
                                              |    "cod",
                                              |    "haddock"
                                              |  ]
                                              |}""".stripMargin)

      JsonBodySpecialCaseHelper
        .extractInteractionsTuple(parse(PactFileExamples.simpleAsString).right.value)
        .right
        .value should ===(
        List(
          (interaction1, interaction1RequestBody, interaction1ResponseBody),
          (interaction2, interaction2RequestBody, interaction2ResponseBody)
        )
      )

    }

  }

}

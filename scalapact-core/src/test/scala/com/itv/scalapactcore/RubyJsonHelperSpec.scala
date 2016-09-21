package com.itv.scalapactcore

import org.scalatest.{FunSpec, Matchers}

class RubyJsonHelperSpec extends FunSpec with Matchers {

  describe("Handling ruby json") {

    it("should be able to extract the provider") {

      JsonBodySpecialCaseHelper.extractPactActor("provider")(PactFileExamples.simpleAsString) shouldEqual Some(PactActor("provider"))

    }

    it("should be able to extract the consumer") {

      JsonBodySpecialCaseHelper.extractPactActor("consumer")(PactFileExamples.simpleAsString) shouldEqual Some(PactActor("consumer"))

    }

    it("should be able to extract a list of interactions paired with their bodies") {

      val interaction1 = Interaction(
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
              "$.headers.Accept" -> MatchingRule(`match` = Option("regex"), regex = Option("\\w+"), min = None),
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
              "$.headers.Accept" -> MatchingRule(`match` = Option("regex"), regex = Option("\\w+"), min = None),
              "$.headers.Content-Length" -> MatchingRule(`match` = Option("type"), regex = None, min = None)
            )
          )
        )
      )
      val interaction1RequestBody = Option("fish")
      val interaction1ResponseBody = Option("""{"fish":["cod","haddock","flying"]}""")

      val interaction2 = Interaction(
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
      val interaction2RequestBody = Option("fish")
      val interaction2ResponseBody = Option("""{"chips":true,"fish":["cod","haddock"]}""")

      val list = List(
        (Some(interaction1), interaction1RequestBody, interaction1ResponseBody),
        (Some(interaction2), interaction2RequestBody, interaction2ResponseBody)
      )

      JsonBodySpecialCaseHelper.extractInteractions(PactFileExamples.simpleAsString) shouldEqual Some(list)

    }

  }

}

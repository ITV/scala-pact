package com.itv.scalapact.circe13

import com.itv.scalapact.shared._
import org.scalatest.{FunSpec, Matchers}
import com.itv.scalapact.test.PactFileExamples

class RubyJsonHelperSpec extends FunSpec with Matchers {

  describe("Handling ruby json") {

    it("should be able to extract provider, consumer and list of interactions") {
      val decodedPact = pactReaderInstance.jsonStringToScalaPact(PactFileExamples.simpleAsString).toOption

      val interaction1 = Interaction(
        providerState = Option("a simple state"),
        description = "a simple request",
        request = InteractionRequest(
          method = Option("GET"),
          path = Option("/fetch-json"),
          query = Option("fish=chips"),
          headers = Option(Map("Content-Type" -> "text/plain")),
          body = Option("fish"),
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
          body = Option("""{
                          |  "fish" : [
                          |    "cod",
                          |    "haddock",
                          |    "flying"
                          |  ]
                          |}""".stripMargin),
          matchingRules = Option(
            Map(
              "$.headers.Accept"         -> MatchingRule(`match` = Option("regex"), regex = Option("\\w+"), min = None),
              "$.headers.Content-Length" -> MatchingRule(`match` = Option("type"), regex = None, min = None)
            )
          )
        )
      )

      val interaction2 = Interaction(
        providerState = Option("a simple state 2"),
        description = "a simple request 2",
        request = InteractionRequest(
          method = Option("GET"),
          path = Option("/fetch-json2"),
          query = None,
          headers = Option(Map("Content-Type" -> "text/plain")),
          body = Option("fish"),
          matchingRules = None
        ),
        response = InteractionResponse(
          status = Option(200),
          headers = Option(Map("Content-Type" -> "application/json")),
          body = Option("""{
                          |  "chips" : true,
                          |  "fish" : [
                          |    "cod",
                          |    "haddock"
                          |  ]
                          |}""".stripMargin),
          matchingRules = None
        )
      )

      decodedPact.map(_.provider) shouldEqual Some(PactActor("provider"))
      decodedPact.map(_.consumer) shouldEqual Some(PactActor("consumer"))
      decodedPact.map(_.interactions) shouldEqual Some(List(interaction1, interaction2))

    }
  }
}

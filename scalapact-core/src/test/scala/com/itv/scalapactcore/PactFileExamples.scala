package com.itv.scalapactcore

object PactFileExamples {

  val verySimple = Pact(
    consumer = PactActor("consumer"),
    provider = PactActor("provider"),
    interactions = List(
      Interaction(
        providerState = None,
        description = "a simple request",
        request = InteractionRequest(
          method = Option("GET"),
          path = Option("/"),
          query = None,
          headers = None,
          body = None,
          matchingRules = None
        ),
        response = InteractionResponse(
          status = Option(200),
          headers = None,
          body = Option("""Hello"""),
          None
        )
      )
    )
  )

  val verySimpleAsString: String =
    """{
      |  "provider" : {
      |    "name" : "provider"
      |  },
      |  "consumer" : {
      |    "name" : "consumer"
      |  },
      |  "interactions" : [
      |    {
      |      "description" : "a simple request",
      |      "request" : {
      |        "method" : "GET",
      |        "path" : "/"
      |      },
      |      "response" : {
      |        "status" : 200,
      |        "body" : "Hello"
      |      }
      |    }
      |  ]
      |}""".stripMargin

  val simple = Pact(
    consumer = PactActor("consumer"),
    provider = PactActor("provider"),
    interactions = List(
      Interaction(
        providerState = Option("a simple state"),
        description = "a simple request",
        request = InteractionRequest(
          method = Option("GET"),
          path = Option("/fetch-json"),
          query = Option("fish=chips"),
          headers = Option(Map("Content-Type" -> "text/plain")),
          body = Option("""fish"""),
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
          body = Option("""{"fish":["cod","haddock","flying"]}"""),
          matchingRules = Option(
            Map(
              "$.headers.Accept" -> MatchingRule(`match` = Option("regex"), regex = Option("\\w+"), min = None),
              "$.headers.Content-Length" -> MatchingRule(`match` = Option("type"), regex = None, min = None)
            )
          )
        )
      ),
      Interaction(
        providerState = Option("a simple state 2"),
        description = "a simple request 2",
        request = InteractionRequest(
          method = Option("GET"),
          path = Option("/fetch-json2"),
          query = None,
          headers = Option(Map("Content-Type" -> "text/plain")),
          body = Option("""fish"""),
          matchingRules = None
        ),
        response = InteractionResponse(
          status = Option(200),
          headers = Option(Map("Content-Type" -> "application/json")),
          body = Option("""{"chips":true,"fish":["cod","haddock"]}"""),
          matchingRules = None
        )
      )
    )
  )

  val simpleAsString: String = """{
                         |  "provider" : {
                         |    "name" : "provider"
                         |  },
                         |  "consumer" : {
                         |    "name" : "consumer"
                         |  },
                         |  "interactions" : [
                         |    {
                         |      "providerState" : "a simple state",
                         |      "description" : "a simple request",
                         |      "request" : {
                         |        "method" : "GET",
                         |        "body" : "fish",
                         |        "path" : "/fetch-json",
                         |        "matchingRules" : {
                         |          "$.headers.Accept" : {
                         |            "match" : "regex",
                         |            "regex" : "\\w+"
                         |          },
                         |          "$.headers.Content-Length" : {
                         |            "match" : "type"
                         |          }
                         |        },
                         |        "query" : "fish=chips",
                         |        "headers" : {
                         |          "Content-Type" : "text/plain"
                         |        }
                         |      },
                         |      "response" : {
                         |        "status" : 200,
                         |        "headers" : {
                         |          "Content-Type" : "application/json"
                         |        },
                         |        "body" : {
                         |          "fish" : [
                         |            "cod",
                         |            "haddock",
                         |            "flying"
                         |          ]
                         |        },
                         |        "matchingRules" : {
                         |          "$.headers.Accept" : {
                         |            "match" : "regex",
                         |            "regex" : "\\w+"
                         |          },
                         |          "$.headers.Content-Length" : {
                         |            "match" : "type"
                         |          }
                         |        }
                         |      }
                         |    },
                         |    {
                         |      "providerState" : "a simple state 2",
                         |      "description" : "a simple request 2",
                         |      "request" : {
                         |        "method" : "GET",
                         |        "body" : "fish",
                         |        "path" : "/fetch-json2",
                         |        "headers" : {
                         |          "Content-Type" : "text/plain"
                         |        }
                         |      },
                         |      "response" : {
                         |        "status" : 200,
                         |        "headers" : {
                         |          "Content-Type" : "application/json"
                         |        },
                         |        "body" : {
                         |          "chips" : true,
                         |          "fish" : [
                         |            "cod",
                         |            "haddock"
                         |          ]
                         |        }
                         |      }
                         |    }
                         |  ]
                         |}""".stripMargin

}

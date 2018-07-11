package com.itv.scalapact.argonaut62

import com.itv.scalapact.shared._

object PactFileExamples {

  private val simpleInteraction = Interaction(
    provider_state = None,
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
  val verySimple = Pact(
    consumer = PactActor("consumer"),
    provider = PactActor("provider"),
    interactions = List(
      simpleInteraction
    ),
    messages = List.empty
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
        provider_state = None,
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
              "$.headers.Accept"         -> MatchingRule(`match` = Option("regex"), regex = Option("\\w+"), min = None),
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
              "$.headers.Accept"         -> MatchingRule(`match` = Option("regex"), regex = Option("\\w+"), min = None),
              "$.headers.Content-Length" -> MatchingRule(`match` = Option("type"), regex = None, min = None)
            )
          )
        )
      ),
      Interaction(
        provider_state = None,
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
    ),
    messages = List.empty
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
                         |      "description" : "a simple request",
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
                         |      },
                         |      "providerState" : "a simple state"
                         |    },
                         |    {
                         |      "request" : {
                         |        "method" : "GET",
                         |        "body" : "fish",
                         |        "path" : "/fetch-json2",
                         |        "headers" : {
                         |          "Content-Type" : "text/plain"
                         |        }
                         |      },
                         |      "description" : "a simple request 2",
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
                         |      },
                         |      "providerState" : "a simple state 2"
                         |    }
                         |  ]
                         |}""".stripMargin

  val simpleOldProviderStateAsString: String = """{
                                 |  "provider" : {
                                 |    "name" : "provider"
                                 |  },
                                 |  "consumer" : {
                                 |    "name" : "consumer"
                                 |  },
                                 |  "interactions" : [
                                 |    {
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
                                 |      "description" : "a simple request",
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
                                 |      },
                                 |      "provider_state" : "a simple state"
                                 |    },
                                 |    {
                                 |      "request" : {
                                 |        "method" : "GET",
                                 |        "body" : "fish",
                                 |        "path" : "/fetch-json2",
                                 |        "headers" : {
                                 |          "Content-Type" : "text/plain"
                                 |        }
                                 |      },
                                 |      "description" : "a simple request 2",
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
                                 |      },
                                 |      "provider_state" : "a simple state 2"
                                 |    }
                                 |  ]
                                 |}""".stripMargin

  val simpleMessage = Pact(
    consumer = PactActor("Consumer"),
    provider = PactActor("Provider"),
    interactions = List.empty,
    messages = List(
      Message(
        description = "Published credit data",
        providerState = Some("or maybe 'scenario'? not sure about this"),
        contents = """{"foo":"bar","number":123}""",
        metaData = Message.Metadata("contentType" -> "application/json"),
        matchingRules = Map("foo"                 -> MatchingRule(Some("regex"), Some("\\w+"), None),
                            "number"              -> MatchingRule(Some("integer"), None, None)),
        contentType = MessageContentType.ApplicationJson,
      )
    )
  )

  //TODO Review how to generate the new matching rules
  //http://pactbroker.stg.fp.itv.com/pacts/provider/Craft%20Backend/consumer/TVT/latest
  val simpleMessageAsString = """{
                                |    "consumer": {
                                |        "name": "Consumer"
                                |    },
                                |    "provider": {
                                |        "name": "Provider"
                                |    },
                                |    "messages": [
                                |        {
                                |            "description": "Published credit data",
                                |            "providerState": "or maybe 'scenario'? not sure about this",
                                |            "contents": {"foo":"bar","number":123},
                                |            "metaData": {
                                |              "contentType": "application/json"
                                |            },
                                |            "matchingRules" : {
                                |               "foo" : {
                                |                 "match":"regex",
                                |                 "regex": "\\w+"
                                |               },
                                |               "number" : {
                                |                  "match": "integer" 
                                |               }
                                |            }
                                |        }
                                |    ]
                                |}""".stripMargin

  val multipleMessage = simpleMessage.copy(
    messages = simpleMessage.messages ++ List(
      Message(
        description = "Published another credit data",
        providerState = Some("or maybe 'scenario'! not sure about this"),
        contents = """{"boo":"xxx"}""",
        metaData = Message.Metadata("contentType" -> "application/json"),
        matchingRules = Map.empty,
        MessageContentType.ApplicationJson
      )
    )
  )
  val multipleMessageAsString = """{
                                |    "consumer": {
                                |        "name": "Consumer"
                                |    },
                                |    "provider": {
                                |        "name": "Provider"
                                |    },
                                |    "messages": [
                                |        {
                                |            "description": "Published credit data",
                                |            "providerState": "or maybe 'scenario'? not sure about this",
                                |            "contents": {"foo":"bar","number":123},
                                |            "metaData": {
                                |              "contentType": "application/json"
                                |            },
                                |            "matchingRules" : {
                                |               "foo" : {
                                |                 "match":"regex",
                                |                 "regex": "\\w+"
                                |               },
                                |               "number" : {
                                |                  "match": "integer"
                                |               }
                                |             }
                                |        },
                                |        {
                                |            "description": "Published another credit data",
                                |            "providerState": "or maybe 'scenario'! not sure about this",
                                |            "contents": {"boo":"xxx"},
                                |            "metaData": {
                                |              "contentType": "application/json"
                                |            }
                                |        }
                                |    ]
                                |}""".stripMargin

  val multipleMessagesAndInteractions = multipleMessage
    .copy(interactions = List(simpleInteraction))

  val multipleMessagesAndInteractionsAsString =
    """{
      |    "consumer": {
      |        "name": "Consumer"
      |    },
      |    "provider": {
      |        "name": "Provider"
      |    },
      |    "messages": [
      |        {
      |            "description": "Published credit data",
      |            "providerState": "or maybe 'scenario'? not sure about this",
      |            "contents": {"foo":"bar","number":123},
      |            "metaData": {
      |              "contentType": "application/json"
      |            },
      |            "matchingRules" : {
      |               "foo" : {
      |                 "match":"regex",
      |                 "regex": "\\w+"
      |               },
      |               "number" : {
      |                  "match": "integer"
      |               }
      |             }
      |        },
      |        {
      |            "description": "Published another credit data",
      |            "providerState": "or maybe 'scenario'! not sure about this",
      |            "contents": {"boo":"xxx"},
      |            "metaData": {
      |              "contentType": "application/json"
      |            }
      |        }
      |    ],
      |    "interactions": [
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
      |     }
      |    ]
      | }
    """.stripMargin
}

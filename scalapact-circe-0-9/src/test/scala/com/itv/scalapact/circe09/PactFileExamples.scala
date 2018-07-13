package com.itv.scalapact.circe09

import com.itv.scalapact.shared.MessageContentType.{ApplicationJson, ApplicationText}
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
                                 |      "providerState" : "a simple state",
                                 |      "description" : "a simple request",
                                 |      "request" : {
                                 |        "method" : "GET",
                                 |        "path" : "/fetch-json",
                                 |        "query" : "fish=chips",
                                 |        "headers" : {
                                 |          "Content-Type" : "text/plain"
                                 |        },
                                 |        "body" : "fish",
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
                                 |        "path" : "/fetch-json2",
                                 |        "headers" : {
                                 |          "Content-Type" : "text/plain"
                                 |        },
                                 |        "body" : "fish"
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

  val simpleOldProviderStateAsString: String = """{
                                                 |  "provider" : {
                                                 |    "name" : "provider"
                                                 |  },
                                                 |  "consumer" : {
                                                 |    "name" : "consumer"
                                                 |  },
                                                 |  "interactions" : [
                                                 |    {
                                                 |      "provider_state" : "a simple state",
                                                 |      "description" : "a simple request",
                                                 |      "request" : {
                                                 |        "method" : "GET",
                                                 |        "path" : "/fetch-json",
                                                 |        "body" : "fish",
                                                 |        "query" : "fish=chips",
                                                 |        "headers" : {
                                                 |          "Content-Type" : "text/plain"
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
                                                 |      "provider_state" : "a simple state 2",
                                                 |      "description" : "a simple request 2",
                                                 |      "request" : {
                                                 |        "method" : "GET",
                                                 |        "path" : "/fetch-json2",
                                                 |        "body" : "fish",
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

  val stringMessage = Pact(
    consumer = PactActor("Consumer"),
    provider = PactActor("Provider"),
    interactions = List.empty,
    messages = List(
      Message(
        description = "Published credit data",
        providerState = Some("or maybe 'scenario'? not sure about this"),
        contents = """Hello world!""",
        metaData = Message.Metadata(),
        matchingRules =
          Map("body" -> Map("$.foo" -> Message.Matchers(List(MatchingRule(Some("regex"), Some("\\w+"), None))))),
        ApplicationText
      )
    )
  )

  val stringMessageAsString = """{
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
                                |            "contents": "Hello world!",
                                |            "metaData": {
                                |            },
                                |            "matchingRules" : {
                                |              "body" : {
                                |                "$.foo" : {
                                |                  "matchers": [
                                |                    {
                                |                      "match":"regex",
                                |                      "regex": "\\w+"
                                |                    }
                                |                  ]
                                |                }
                                |              }
                                |            }
                                |        }
                                |    ]
                                |}""".stripMargin

  val jsonMessage = Pact(
    consumer = PactActor("Consumer"),
    provider = PactActor("Provider"),
    interactions = List.empty,
    messages = List(
      Message(
        description = "Published credit data",
        providerState = Some("or maybe 'scenario'? not sure about this"),
        contents = """{"foo":"bar"}""",
        metaData = Message.Metadata("contentType" -> "application/json"),
        matchingRules = Map.empty,
        ApplicationJson
      )
    )
  )

  val jsonMessageAsString = """{
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
                              |            "contents": {"foo":"bar"},
                              |            "metaData": {
                              |              "contentType": "application/json"
                              |            }
                              |        }
                              |    ]
                              |}""".stripMargin

  val multipleMessage = jsonMessage.copy(
    messages = jsonMessage.messages ++ List(
      Message(
        description = "Published another credit data",
        providerState = Some("or maybe 'scenario'! not sure about this"),
        contents = """{"boo":"xxx","foo":123}""",
        metaData = Message.Metadata("contentType" -> "application/json"),
        matchingRules = Map(
          "body" ->
            Map(
              "$.foo" -> Message.Matchers(List(MatchingRule(Some("integer"), None, None)))
            )
        ),
        ApplicationJson
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
                                  |            "contents": {"foo":"bar"},
                                  |            "metaData": {
                                  |              "contentType": "application/json"
                                  |            }
                                  |        },
                                  |        {
                                  |            "description": "Published another credit data",
                                  |            "providerState": "or maybe 'scenario'! not sure about this",
                                  |            "contents": {"boo":"xxx","foo":123},
                                  |            "matchingRules" : {
                                  |              "body" : {
                                  |                "$.foo" : {
                                  |                  "matchers": [
                                  |                    {
                                  |                      "match":"integer"
                                  |                    }
                                  |                  ]
                                  |                }
                                  |              }
                                  |            },
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
      |            "contents": {"foo":"bar"},
      |            "metaData": {
      |              "contentType": "application/json"
      |            }
      |        },
      |        {
      |            "description": "Published another credit data",
      |            "providerState": "or maybe 'scenario'! not sure about this",
      |            "contents": {"boo":"xxx","foo":123},
      |            "matchingRules" : {
      |              "body" : {
      |                "$.foo" : {
      |                  "matchers": [
      |                    {
      |                      "match":"integer"
      |                    }
      |                  ]
      |                }
      |              }
      |            },
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

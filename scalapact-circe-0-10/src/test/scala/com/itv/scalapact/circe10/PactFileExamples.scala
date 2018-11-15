package com.itv.scalapact.circe10

import com.itv.scalapact.shared._

object PactFileExamples {

  val anotherExample: String =
    """{
      |  "provider" : {
      |    "name" : "Their Provider Service"
      |  },
      |  "consumer" : {
      |    "name" : "My Consumer"
      |  },
      |  "interactions" : [
      |    {
      |      "description" : "a simple get example with a header matcher",
      |      "request" : {
      |        "method" : "GET",
      |        "path" : "/header-match",
      |        "headers" : {
      |          "fish" : "chips",
      |          "sauce" : "ketchup"
      |        },
      |        "matchingRules" : {
      |          "$.headers.fish" : {
      |            "match" : "regex",
      |            "regex" : "\\w+"
      |          },
      |          "$.headers.sauce" : {
      |            "match" : "regex",
      |            "regex" : "\\w+"
      |          }
      |        }
      |      },
      |      "response" : {
      |        "status" : 200,
      |        "headers" : {
      |          "fish" : "chips",
      |          "sauce" : "ketchup"
      |        },
      |        "body" : "Hello there!",
      |        "matchingRules" : {
      |          "$.headers.fish" : {
      |            "match" : "regex",
      |            "regex" : "\\w+"
      |          },
      |          "$.headers.sauce" : {
      |            "match" : "regex",
      |            "regex" : "\\w+"
      |          }
      |        }
      |      }
      |    }
      |  ]
      |}""".stripMargin

  val verySimple = Pact(
    provider = PactActor("provider"),
    consumer = PactActor("consumer"),
    interactions = List(
      Interaction(
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
    ),
    _links = None,
    metadata = None
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
    provider = PactActor("provider"),
    consumer = PactActor("consumer"),
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
    _links = None,
    metadata = None
  )

  val _links = Map(
    "self" -> LinkValues(
      title = Option("Pact"),
      name = Option("Pact between consumer (v1.0.0) and provider"),
      href = "http://localhost/pacts/provider/provider/consumer/consumer/version/1.0.0",
      templated = None
    ),
    "pb:consumer" -> LinkValues(
      title = Option("Consumer"),
      name = Option("consumer"),
      href = "http://localhost/pacticipants/consumer",
      templated = None
    ),
    "pb:provider" -> LinkValues(
      title = Option("Provider"),
      name = Option("provider"),
      href = "http://localhost/pacticipants/provider",
      templated = None
    ),
    "pb:latest-tagged-pact-version" -> LinkValues(
      title = Option("Latest tagged version of this pact"),
      name = None,
      href = "http://localhost/pacts/provider/provider-service/consumer/consumer-service/latest/{tag}",
      templated = Option(true)
    )
  )

  val simpleWithLinksAndMetaData: Pact =
    simple.copy(
      _links = Option(_links),
      metadata = Option(
        PactMetaData(
          pactSpecification = Option(VersionMetaData("2.0.0")),
          `scala-pact` = Option(VersionMetaData("1.0.0"))
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

  val simpleWithLinksAndMetaDataAsString: String =
    """{
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
      |  ],
      |  "_links": {
      |    "self": {
      |      "title": "Pact",
      |      "name": "Pact between consumer (v1.0.0) and provider",
      |      "href": "http://localhost/pacts/provider/provider/consumer/consumer/version/1.0.0"
      |    },
      |    "pb:consumer": {
      |      "title": "Consumer",
      |      "name": "consumer",
      |      "href": "http://localhost/pacticipants/consumer"
      |    },
      |    "pb:provider": {
      |      "title": "Provider",
      |      "name": "provider",
      |      "href": "http://localhost/pacticipants/provider"
      |    },
      |    "pb:latest-tagged-pact-version": {
      |      "title": "Latest tagged version of this pact",
      |      "href": "http://localhost/pacts/provider/provider-service/consumer/consumer-service/latest/{tag}",
      |      "templated": true
      |    },
      |    "curies": [
      |      {
      |        "name": "pb",
      |        "href": "http://localhost/doc/{rel}",
      |        "templated": true
      |      }
      |    ]
      |  },
      |  "metadata": {
      |    "pactSpecification": {
      |      "version": "2.0.0"
      |    },
      |    "scala-pact": {
      |      "version": "1.0.0"
      |    }
      |  }
      |}""".stripMargin

  val simpleWithMetaDataAsString: String =
    """{
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
      |  ],
      |  "metadata": {
      |    "pactSpecification": {
      |      "version": "2.0.0"
      |    },
      |    "scala-pact": {
      |      "version": "1.0.0"
      |    }
      |  }
      |}""".stripMargin

}

package com.itv.scalapactcore

object PactFileExamples {

  val simpleBody = """{"fish":["cod", "haddock", "flying"]}"""

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
          headers = Option(Map("Content-Type" -> "text/plain")),
          body = Option("""fish""")
        ),
        response = InteractionResponse(
          status = Option(200),
          headers = Option(Map("Content-Type" -> "application/json")),
          body = Option(simpleBody)
        )
      )
    )
  )

  val simpleAsString =
    """
      |{
      |  "consumer":"consumer",
      |  "provider":"provider",
      |  "interactions":[
      |    {
      |      "providerState":"a simple state",
      |      "description":"a simple request",
      |      "request": {
      |        "method":"GET",
      |        "path":"/fetch-json",
      |        "headers": {
      |          "Content-Type":"text/plain"
      |        },
      |        "body":"fish"
      |      },
      |      "response": {
      |        "status":200,
      |        "headers": {
      |          "Content-Type":"application/json"
      |        },
      |        "body": {
      |          "fish": [
      |            "cod",
      |            "haddock",
      |            "flying"
      |          ]
      |        }
      |      }
      |    }
      |  ]
      |}
    """.stripMargin

}

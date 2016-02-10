package com.itv.scalapact



object ScalaPactBuilder {

  object makePact {
    def context(key: String): ScalaPactContext = new ScalaPactContext(key)

    private class ScalaPactContext(context: String) {
      def consumer(name: String): ScalaPartialPact = new ScalaPartialPact(context, name)
    }

    private class ScalaPartialPact(context: String, consumer: String) {
      def hasPactWith(provider: String): ScalaPactDescription = new ScalaPactDescription(context, consumer, provider, Nil)
    }

    private class ScalaPactDescription(context: String, consumer: String, provider: String, interactions: List[ScalaPactInteraction]) {
      def addInteraction(interaction: ScalaPactInteraction): ScalaPactDescription = new ScalaPactDescription(context, consumer, provider, interactions ++ List(interaction))

      //TODO: Pass in all of the above to the runConsumerTest method
      def runConsumerTest(): Unit = ()
    }
  }

  object interaction {
    def description(message: String): ScalaPactInteraction = new ScalaPactInteraction(message, None, None, None)
  }

  private class ScalaPactInteraction(description: String, providerState: Option[String], request: Option[ScalaPactRequest], response: Option[ScalaPactResponse]) {
    def given(state: String): ScalaPactInteraction = new ScalaPactInteraction(description, Option(state), None, None)
    def uponReceiving(method: String, path: String, headers: Map[String, String], body: Option[String]): ScalaPactInteraction = new ScalaPactInteraction(
      description,
      providerState,
      Option(ScalaPactRequest(method, path, headers, body)),
      response
    )
    def willRespondWith(status: Int, headers: Map[String, String], body: Option[String]): ScalaPactInteraction = new ScalaPactInteraction(
      description,
      providerState,
      request,
      Option(ScalaPactResponse(status, headers, body))
    )
  }

  private case class ScalaPactRequest(method: String, path: String, headers: Map[String, String], body: Option[String])
  private case class ScalaPactResponse(status: Int, headers: Map[String, String], body: Option[String])

}

/*


  makePact
    .context("My contextual description")
    .consumer("My Consumer")
    .hasPactWith("My Provider")
    .addInteraction(
      interaction
        .description("An interaction")
        .given("Some state")
        .uponReceiving("GET", "/", Map.empty, None)
        .willRespondWith(200, Map.empty, None)
    )
    .runConsumerTest()
 */

/*
PactBuilder(pactContext = "Simple get example")
        .consumer("My Consumer")
        .hasPactWith("Their Provider Service")
        .withInteraction(
          PactInteraction(
            description = "Fetch a greeting",
            given = None,
            uponReceivingRequest
              .path(endPoint),
            willRespondWith
              .status(200)
              .body("Hello there!")
          )
        )
        .withConsumerTest { scalaPactMockConfig =>

          val result = SimpleClient.doGetRequest(scalaPactMockConfig.baseUrl, endPoint, Map())

          result.status should equal(200)
          result.body should equal("Hello there!")
        }
 */
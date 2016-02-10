package com.itv.scalapact

object ScalaPactForger {

  implicit val options = ScalaPactOptions.DefaultOptions

  object forgePact {
    def between(consumer: String): ScalaPartialPact = new ScalaPartialPact(consumer)

    private class ScalaPartialPact(consumer: String) {
      def and(provider: String): ScalaPartialPactWith = new ScalaPartialPactWith(consumer, provider)
    }

    private class ScalaPartialPactWith(consumer: String, provider: String) {
      def describing(scenario: String): ScalaPactDescription = new ScalaPactDescription(scenario, consumer, provider, Nil)
    }

    private class ScalaPactDescription(context: String, consumer: String, provider: String, interactions: List[ScalaPactInteraction]) {
      def addInteraction(interaction: ScalaPactInteraction): ScalaPactDescription = new ScalaPactDescription(context, consumer, provider, interactions ++ List(interaction))

      //TODO: Pass in all of the above to the runConsumerTest method
      def runConsumerTest(test: ScalaPactMockConfig => Unit)(implicit options: ScalaPactOptions): Unit = {
        ScalaPactMock.runConsumerIntegrationTest(
          ScalaPactDescriptionFinal(
            context,
            consumer,
            provider,
            interactions.map(i => i.finalise),
            options
          )
        )(test)
      }
    }
  }

  object interaction {
    def description(message: String): ScalaPactInteraction = new ScalaPactInteraction(message, None, None, None)
  }

  private class ScalaPactInteraction(description: String, providerState: Option[String], request: Option[ScalaPactRequest], response: Option[ScalaPactResponse]) {
    def given(state: String): ScalaPactInteraction = new ScalaPactInteraction(description, Option(state), None, None)


    def uponReceiving(path: String): ScalaPactInteraction = uponReceiving(GET, path, Map.empty, None)
    def uponReceiving(method: ScalaPactMethod, path: String): ScalaPactInteraction = uponReceiving(method, path, Map.empty, None)
    def uponReceiving(method: ScalaPactMethod, path: String, headers: Map[String, String], body: Option[String]): ScalaPactInteraction = new ScalaPactInteraction(
      description,
      providerState,
      Option(ScalaPactRequest(method, path, headers, body)),
      response
    )

    def willRespondWith(status: Int): ScalaPactInteraction = willRespondWith(status, Map.empty, None)
    def willRespondWith(status: Int, body: String): ScalaPactInteraction = willRespondWith(status, Map.empty, Option(body))
    def willRespondWith(status: Int, headers: Map[String, String], body: Option[String]): ScalaPactInteraction = new ScalaPactInteraction(
      description,
      providerState,
      request,
      Option(ScalaPactResponse(status, headers, body))
    )

    def finalise: ScalaPactInteractionFinal = ScalaPactInteractionFinal(description, providerState, request, response)
  }

  case class ScalaPactDescriptionFinal(context: String, consumer: String, provider: String, interactions: List[ScalaPactInteractionFinal], options: ScalaPactOptions)
  case class ScalaPactInteractionFinal(description: String, providerState: Option[String], request: Option[ScalaPactRequest], response: Option[ScalaPactResponse])

  case class ScalaPactRequest(method: ScalaPactMethod, path: String, headers: Map[String, String], body: Option[String])
  case class ScalaPactResponse(status: Int, headers: Map[String, String], body: Option[String])

  object ScalaPactOptions {
    val DefaultOptions = ScalaPactOptions(writePactFiles = true)
  }
  case class ScalaPactOptions(writePactFiles: Boolean)

  sealed trait ScalaPactMethod {
    val method: String
  }
  case object GET extends ScalaPactMethod { val method = "GET" }
  case object PUT extends ScalaPactMethod { val method = "PUT" }
  case object POST extends ScalaPactMethod { val method = "POST" }
  case object DELETE extends ScalaPactMethod { val method = "DELETE" }

}
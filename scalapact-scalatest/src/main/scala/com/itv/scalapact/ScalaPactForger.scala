package com.itv.scalapact

import com.itv.scalapact.shared.{ContextNameAndClientAuth, SslContextMap}

import scala.language.implicitConversions
import scala.util.Properties
import com.itv.scalapact.shared.Maps._

object ScalaPactForger {

  implicit def toOption[A](a: A): Option[A] = Option(a)

  implicit def rulesToOptionalList(rules: ScalaPactForger.ScalaPactMatchingRules): Option[List[ScalaPactForger.ScalaPactMatchingRule]] =
    Option(rules.rules)

  implicit val options: ScalaPactOptions = ScalaPactOptions.DefaultOptions

  object forgePact extends ForgePactElements {
    protected val strict: Boolean = false
  }

  object forgeStrictPact extends ForgePactElements {
    protected val strict: Boolean = true
  }

  sealed trait ForgePactElements {
    protected val strict: Boolean

    def between(consumer: String): ScalaPartialPact = new ScalaPartialPact(consumer)

    class ScalaPartialPact(consumer: String) {
      def and(provider: String): ScalaPactDescription = new ScalaPactDescription(consumer, provider, None, Nil)
    }

    class ScalaPactDescription(consumer: String, provider: String,  optContextNameAndClientAuth: Option[ContextNameAndClientAuth], interactions: List[ScalaPactInteraction]) {

      /**
        * Adds interactions to the Pact. Interactions should be created using the helper object 'interaction'
        *
        * @param interaction [ScalaPactInteraction] definition
        * @return [ScalaPactDescription] to allow the builder to continue
        */
      def addInteraction(interaction: ScalaPactInteraction): ScalaPactDescription = new ScalaPactDescription(consumer, provider, optContextNameAndClientAuth, interactions ++ List(interaction))

      def addSslContextForServer(name: String, clientAuth: Boolean): ScalaPactDescription = new ScalaPactDescription(consumer, provider, Some(ContextNameAndClientAuth(name, clientAuth)), interactions)

      def runConsumerTest[A](test: ScalaPactMockConfig => A)(implicit options: ScalaPactOptions, sslContextMap: SslContextMap): A = {
        ScalaPactMock.runConsumerIntegrationTest(strict)(
          ScalaPactDescriptionFinal(
            consumer,
            provider,
            optContextNameAndClientAuth,
            interactions.map(i => i.finalise),
            options
          )
        )(test)
      }

    }

  }

  object interaction {
    def description(message: String): ScalaPactInteraction = new ScalaPactInteraction(message, None, None, ScalaPactRequest.default, ScalaPactResponse.default)
  }

  class ScalaPactInteraction(description: String, providerState: Option[String], optContextNameAndClientAuth: Option[ContextNameAndClientAuth], request: ScalaPactRequest, response: ScalaPactResponse) {
    def given(state: String): ScalaPactInteraction = new ScalaPactInteraction(description, Option(state), optContextNameAndClientAuth, request, response)

    def withSsl(sslContextName: String, clientAuth: Boolean): ScalaPactInteraction = new ScalaPactInteraction(description, providerState, Some(ContextNameAndClientAuth(sslContextName, clientAuth)), request, response)

    def uponReceiving(path: String): ScalaPactInteraction = uponReceiving(GET, path, None, Map.empty, None, None)

    def uponReceiving(method: ScalaPactMethod, path: String): ScalaPactInteraction = uponReceiving(method, path, None, Map.empty, None, None)

    def uponReceiving(method: ScalaPactMethod, path: String, query: Option[String]): ScalaPactInteraction = uponReceiving(method, path, query, Map.empty, None, None)

    def uponReceiving(method: ScalaPactMethod, path: String, query: Option[String], headers: Map[String, String], body: Option[String], matchingRules: Option[List[ScalaPactMatchingRule]]): ScalaPactInteraction = new ScalaPactInteraction(
      description,
      providerState,
      optContextNameAndClientAuth,
      ScalaPactRequest(method, path, query, headers, body, matchingRules),
      response
    )

    def willRespondWith(status: Int): ScalaPactInteraction = willRespondWith(status, Map.empty, None, None)

    def willRespondWith(status: Int, body: String): ScalaPactInteraction = willRespondWith(status, Map.empty, Option(body), None)

    def willRespondWith(status: Int, headers: Map[String, String], body: String): ScalaPactInteraction = willRespondWith(status, headers, Option(body), None)

    def willRespondWith(status: Int, headers: Map[String, String], body: Option[String], matchingRules: Option[List[ScalaPactMatchingRule]]): ScalaPactInteraction = new ScalaPactInteraction(
      description,
      providerState,
      optContextNameAndClientAuth,
      request,
      ScalaPactResponse(status, headers, body, matchingRules)
    )

    def finalise: ScalaPactInteractionFinal = ScalaPactInteractionFinal(description, providerState, optContextNameAndClientAuth, request, response)
  }

  case class ScalaPactDescriptionFinal(consumer: String, provider: String, optContextNameAndClientAuth: Option[ContextNameAndClientAuth], interactions: List[ScalaPactInteractionFinal], options: ScalaPactOptions) {
    def withHeaderForSsl: ScalaPactDescriptionFinal = copy(interactions = interactions.map(i => i.copy(request = i.request.copy(headers = i.request.headers addOpt (SslContextMap.sslContextHeaderName -> i.optContextNameAndClientAuth.map(_.name))))))
  }

  case class ScalaPactInteractionFinal(description: String, providerState: Option[String], optContextNameAndClientAuth: Option[ContextNameAndClientAuth], request: ScalaPactRequest, response: ScalaPactResponse)

  object ScalaPactRequest {
    val default: ScalaPactRequest = ScalaPactRequest(GET, "/", None, Map.empty, None, None)
  }

  case class ScalaPactRequest(method: ScalaPactMethod, path: String, query: Option[String], headers: Map[String, String], body: Option[String], matchingRules: Option[List[ScalaPactMatchingRule]])

  sealed trait ScalaPactMatchingRule {
    val key: String
  }

  case class ScalaPactMatchingRuleRegex(key: String, regex: String) extends ScalaPactMatchingRule

  case class ScalaPactMatchingRuleType(key: String) extends ScalaPactMatchingRule

  case class ScalaPactMatchingRuleArrayMinLength(key: String, minimum: Int) extends ScalaPactMatchingRule

  case class ScalaPactMatchingRules(rules: List[ScalaPactMatchingRule]) {
    def ~>(newRules: ScalaPactMatchingRules): ScalaPactMatchingRules = ScalaPactMatchingRules(
      rules = rules ++ newRules.rules
    )
  }

  object headerRegexRule {
    def apply(key: String, regex: String): ScalaPactMatchingRules = ScalaPactMatchingRules(
      rules = List(ScalaPactMatchingRuleRegex("$.headers." + key, regex))
    )
  }

  object bodyRegexRule {
    def apply(key: String, regex: String): ScalaPactMatchingRules = ScalaPactMatchingRules(
      rules = List(ScalaPactMatchingRuleRegex("$.body." + key, regex))
    )
  }

  object bodyTypeRule {
    def apply(key: String): ScalaPactMatchingRules = ScalaPactMatchingRules(
      rules = List(ScalaPactMatchingRuleType("$.body." + key))
    )
  }

  object bodyArrayMinimumLengthRule {
    def apply(key: String, minimum: Int): ScalaPactMatchingRules = ScalaPactMatchingRules(
      rules = List(ScalaPactMatchingRuleArrayMinLength("$.body." + key, minimum))
    )
  }

  object ScalaPactResponse {
    val default: ScalaPactResponse = ScalaPactResponse(200, Map.empty, None, None)
  }

  case class ScalaPactResponse(status: Int, headers: Map[String, String], body: Option[String], matchingRules: Option[List[ScalaPactMatchingRule]])

  object ScalaPactOptions {
    val DefaultOptions: ScalaPactOptions = ScalaPactOptions(writePactFiles = true, outputPath = Properties.envOrElse("pact.rootDir", "target/pacts"))
  }

  case class ScalaPactOptions(writePactFiles: Boolean, outputPath: String)

  sealed trait ScalaPactMethod {
    val method: String
  }

  case object GET extends ScalaPactMethod {
    val method = "GET"
  }

  case object PUT extends ScalaPactMethod {
    val method = "PUT"
  }

  case object POST extends ScalaPactMethod {
    val method = "POST"
  }

  case object DELETE extends ScalaPactMethod {
    val method = "DELETE"
  }

  case object OPTIONS extends ScalaPactMethod {
    val method = "OPTIONS"
  }

}

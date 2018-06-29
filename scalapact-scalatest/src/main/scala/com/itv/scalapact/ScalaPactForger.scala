package com.itv.scalapact

import com.itv.scalapact.shared.SslContextMap

import scala.language.implicitConversions
import scala.util.Properties
import com.itv.scalapact.shared.Maps._
import com.itv.scalapact.shared.PactFormat
import com.itv.scalapact.shared.typeclasses.{IPactReader, IPactStubber, IPactWriter, IScalaPactHttpClient}

object ScalaPactForger {

  implicit def toOption[A](a: A): Option[A] = Option(a)

  implicit def rulesToOptionalList(
      rules: ScalaPactForger.ScalaPactMatchingRules
  ): Option[List[ScalaPactForger.ScalaPactMatchingRule]] =
    Option(rules.rules)

  implicit val options: ScalaPactOptions = ScalaPactOptions.DefaultOptions

  object forgePact extends ForgePactElements {
    protected val strict: Boolean = false
  }

  object forgeStrictPact extends ForgePactElements {
    protected val strict: Boolean = true
  }


  case class PartialScalaPactMessage(description: String,
                                    providerState: Option[String],
                                    meta: Map[String, String]) {

    def withProviderState(state: String): PartialScalaPactMessage =
      copy(providerState = Some(state))

    def withMeta(meta: Map[String, String]): PartialScalaPactMessage =
      copy(meta = meta)

    def withContent[T](value: T)(implicit format: PactFormat[T]): ScalaPactMessage =
      ScalaPactMessage(description, providerState, format.encode(value), meta + ("contentType" -> format.contentType))
  }

  case class ScalaPactMessage(description: String,
                              providerState: Option[String],
                              content: Option[String],
                              meta: Map[String, String]) {

    val contentType: Option[String] = meta.get("contentType")
  }

  sealed trait ForgePactElements {
    protected val strict: Boolean

    def between(consumer: String): ScalaPartialPact = new ScalaPartialPact(consumer)

    class ScalaPartialPact(consumer: String) {
      def and(provider: String): ScalaPactDescription = new ScalaPactDescription(consumer, provider, None, Nil, Nil)
    }

    class ScalaPactDescription(consumer: String,
                               provider: String,
                               sslContextName: Option[String],
                               interactions: List[ScalaPactInteraction],
                               messages: List[ScalaPactMessage]) {

      /**
        * Adds interactions to the Pact. Interactions should be created using the helper object 'interaction'
        *
        * @param interaction [ScalaPactInteraction] definition
        * @return [ScalaPactDescription] to allow the builder to continue
        */
      def addInteraction(interaction: ScalaPactInteraction): ScalaPactDescription =
        new ScalaPactDescription(consumer, provider, sslContextName, interactions ++ List(interaction), messages)

      def addSslContextForServer(name: String): ScalaPactDescription =
        new ScalaPactDescription(consumer, provider, Some(name), interactions, messages)

      def runConsumerTest[F[_], A](test: ScalaPactMockConfig => A)(implicit options: ScalaPactOptions,
                                                                   sslContextMap: SslContextMap,
                                                                   pactReader: IPactReader,
                                                                   pactWriter: IPactWriter,
                                                                   httpClient: IScalaPactHttpClient[F],
                                                                   pactStubber: IPactStubber): A =
        ScalaPactMock.runConsumerIntegrationTest(strict)(
          ScalaPactDescriptionFinal(
            consumer,
            provider,
            sslContextName,
            interactions.map(i => i.finalise),
            options
          )
        )(test)

    }

  }
  object message {
    def description(desc: String): ScalaPactMessage =
      ScalaPactMessage(desc, None, None, Map.empty)
  }

  object interaction {
    def description(message: String): ScalaPactInteraction =
      new ScalaPactInteraction(message, None, None, ScalaPactRequest.default, ScalaPactResponse.default)
  }

  class ScalaPactInteraction(description: String,
                             providerState: Option[String],
                             sslContextName: Option[String],
                             request: ScalaPactRequest,
                             response: ScalaPactResponse) {
    def given(state: String): ScalaPactInteraction =
      new ScalaPactInteraction(description, Option(state), sslContextName, request, response)

    def withSsl(sslContextName: String): ScalaPactInteraction =
      new ScalaPactInteraction(description, providerState, Some(sslContextName), request, response)

    def uponReceiving(path: String): ScalaPactInteraction = uponReceiving(GET, path, None, Map.empty, None, None)

    def uponReceiving(method: ScalaPactMethod, path: String): ScalaPactInteraction =
      uponReceiving(method, path, None, Map.empty, None, None)

    def uponReceiving(method: ScalaPactMethod, path: String, query: Option[String]): ScalaPactInteraction =
      uponReceiving(method, path, query, Map.empty, None, None)

    def uponReceiving(method: ScalaPactMethod,
                      path: String,
                      query: Option[String],
                      headers: Map[String, String],
                      body: Option[String],
                      matchingRules: Option[List[ScalaPactMatchingRule]]): ScalaPactInteraction =
      new ScalaPactInteraction(
        description,
        providerState,
        sslContextName,
        ScalaPactRequest(method, path, query, headers, body, matchingRules),
        response
      )

    def willRespondWith(status: Int): ScalaPactInteraction = willRespondWith(status, Map.empty, None, None)

    def willRespondWith(status: Int, body: String): ScalaPactInteraction =
      willRespondWith(status, Map.empty, Option(body), None)

    def willRespondWith(status: Int, headers: Map[String, String], body: String): ScalaPactInteraction =
      willRespondWith(status, headers, Option(body), None)

    def willRespondWith(status: Int,
                        headers: Map[String, String],
                        body: Option[String],
                        matchingRules: Option[List[ScalaPactMatchingRule]]): ScalaPactInteraction =
      new ScalaPactInteraction(
        description,
        providerState,
        sslContextName,
        request,
        ScalaPactResponse(status, headers, body, matchingRules)
      )

    def finalise: ScalaPactInteractionFinal =
      ScalaPactInteractionFinal(description, providerState, sslContextName, request, response)
  }

  case class ScalaPactDescriptionFinal(consumer: String,
                                       provider: String,
                                       serverSslContextName: Option[String],
                                       interactions: List[ScalaPactInteractionFinal],
                                       options: ScalaPactOptions) {
    def withHeaderForSsl: ScalaPactDescriptionFinal =
      copy(
        interactions = interactions.map(
          i =>
            i.copy(
              request = i.request
                .copy(headers = i.request.headers addOpt (SslContextMap.sslContextHeaderName -> i.sslContextName))
          )
        )
      )
  }

  case class ScalaPactInteractionFinal(description: String,
                                       providerState: Option[String],
                                       sslContextName: Option[String],
                                       request: ScalaPactRequest,
                                       response: ScalaPactResponse)

  object ScalaPactRequest {
    val default: ScalaPactRequest = ScalaPactRequest(GET, "/", None, Map.empty, None, None)
  }

  case class ScalaPactRequest(method: ScalaPactMethod,
                              path: String,
                              query: Option[String],
                              headers: Map[String, String],
                              body: Option[String],
                              matchingRules: Option[List[ScalaPactMatchingRule]])

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

  case class ScalaPactResponse(status: Int,
                               headers: Map[String, String],
                               body: Option[String],
                               matchingRules: Option[List[ScalaPactMatchingRule]])

  object ScalaPactOptions {
    val DefaultOptions: ScalaPactOptions =
      ScalaPactOptions(writePactFiles = true, outputPath = Properties.envOrElse("pact.rootDir", "target/pacts"))
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

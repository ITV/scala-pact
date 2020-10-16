package com.itv.scalapact

import com.itv.scalapact.shared.{HttpMethod, SslContextMap}

import scala.util.Properties
import com.itv.scalapact.shared.Maps._
import com.itv.scalapact.shared.typeclasses.{IPactReader, IPactStubber, IPactWriter, IScalaPactHttpClient, IScalaPactHttpClientBuilder}

import scala.concurrent.duration._

object ScalaPactForger {
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

    class ScalaPactDescription(consumer: String,
                               provider: String,
                               sslContextName: Option[String],
                               interactions: List[ScalaPactInteraction]) {

      /**
        * Adds interactions to the Pact. Interactions should be created using the helper object 'interaction'
        *
        * @param interaction [ScalaPactInteraction] definition
        * @return [ScalaPactDescription] to allow the builder to continue
        */
      def addInteraction(interaction: ScalaPactInteraction): ScalaPactDescription =
        new ScalaPactDescription(consumer, provider, sslContextName, interactions :+ interaction)

      def addSslContextForServer(name: String): ScalaPactDescription =
        new ScalaPactDescription(consumer, provider, Some(name), interactions)

      def runConsumerTest[F[_], A](test: ScalaPactMockConfig => A)(implicit options: ScalaPactOptions,
                                                                   sslContextMap: SslContextMap,
                                                                   pactReader: IPactReader,
                                                                   pactWriter: IPactWriter,
                                                                   httpClientBuilder: IScalaPactHttpClientBuilder[F],
                                                                   pactStubber: IPactStubber): A = {
        implicit val client: IScalaPactHttpClient[F] =
          httpClientBuilder.build(2.seconds, sslContextName)
        ScalaPactMock.runConsumerIntegrationTest(strict)(
          finalise(options)
        )(test)
      }

      private def finalise(implicit options: ScalaPactOptions): ScalaPactDescriptionFinal =
        ScalaPactDescriptionFinal(
          consumer,
          provider,
          sslContextName,
          interactions.map(i => i.finalise),
          options
        )
    }

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

    def uponReceiving(path: String): ScalaPactInteraction = uponReceiving(GET, path, None, Map.empty, None, ScalaPactMatchingRules.empty)

    def uponReceiving(method: HttpMethod, path: String): ScalaPactInteraction =
      uponReceiving(method, path, None, Map.empty, None, ScalaPactMatchingRules.empty)

    def uponReceiving(method: HttpMethod, path: String, query: Option[String]): ScalaPactInteraction =
      uponReceiving(method, path, query, Map.empty, None, ScalaPactMatchingRules.empty)

    def uponReceiving(method: HttpMethod,
                      path: String,
                      query: Option[String],
                      headers: Map[String, String]): ScalaPactInteraction =
      uponReceiving(method, path, query, headers, None, ScalaPactMatchingRules.empty)

    def uponReceiving(method: HttpMethod,
                      path: String,
                      query: Option[String],
                      headers: Map[String, String],
                      body: String): ScalaPactInteraction =
      uponReceiving(method, path, query, headers, Some(body), ScalaPactMatchingRules.empty)

    def uponReceiving(method: HttpMethod,
                      path: String,
                      query: Option[String],
                      headers: Map[String, String],
                      body: Option[String],
                      matchingRules: ScalaPactMatchingRules): ScalaPactInteraction =
      new ScalaPactInteraction(
        description,
        providerState,
        sslContextName,
        ScalaPactRequest(method, path, query, headers, body, matchingRules.toOption),
        response
      )

    def willRespondWith(status: Int): ScalaPactInteraction = willRespondWith(status, Map.empty, None, ScalaPactMatchingRules.empty)

    def willRespondWith(status: Int, body: String): ScalaPactInteraction =
      willRespondWith(status, Map.empty, Option(body), ScalaPactMatchingRules.empty)

    def willRespondWith(status: Int, headers: Map[String, String], body: String): ScalaPactInteraction =
      willRespondWith(status, headers, Option(body), ScalaPactMatchingRules.empty)

    def willRespondWith(status: Int,
                        headers: Map[String, String],
                        body: Option[String],
                        matchingRules: ScalaPactMatchingRules): ScalaPactInteraction =
      new ScalaPactInteraction(
        description,
        providerState,
        sslContextName,
        request,
        ScalaPactResponse(status, headers, body, matchingRules.toOption)
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

  case class ScalaPactRequest(method: HttpMethod,
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
    def toOption: Option[List[ScalaPactMatchingRule]] = rules match {
      case Nil => None
      case rs => Some(rs)
    }
  }

  object ScalaPactMatchingRules {
    def empty: ScalaPactMatchingRules = ScalaPactMatchingRules(Nil)
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

  val GET: HttpMethod = HttpMethod.GET
  val POST: HttpMethod = HttpMethod.POST
  val PUT: HttpMethod = HttpMethod.PUT
  val DELETE: HttpMethod = HttpMethod.DELETE
  val OPTIONS: HttpMethod = HttpMethod.OPTIONS
  val PATCH: HttpMethod = HttpMethod.PATCH
  val CONNECT: HttpMethod = HttpMethod.CONNECT
  val TRACE: HttpMethod = HttpMethod.TRACE
  val HEAD: HttpMethod = HttpMethod.HEAD
}

package com.itv.scalapact

import com.itv.scalapact.model.ScalaPactMatchingRule._
import com.itv.scalapact.model._
import com.itv.scalapact.shared.IPactStubber
import com.itv.scalapact.shared.http.{HttpMethod, IScalaPactHttpClient, IScalaPactHttpClientBuilder, SslContextMap}
import com.itv.scalapact.shared.json.{IPactReader, IPactWriter}

import scala.concurrent.duration.DurationInt
import scala.language.implicitConversions

sealed abstract class ScalaPactForgerDsl[PactOps <: PactDescriptionOps] {
  implicit val options: ScalaPactOptions = ScalaPactOptions.DefaultOptions

  object forgePact       extends ForgePactElements(strict = false)
  object forgeStrictPact extends ForgePactElements(strict = true)

  sealed class ForgePactElements(strict: Boolean) {
    def between(consumer: String): ScalaPartialPact = new ScalaPartialPact(consumer)

    class ScalaPartialPact(consumer: String) {
      def and(provider: String): ScalaPactDescription = new ScalaPactDescription(strict, consumer, provider, None, Nil)
    }
  }

  object interaction {
    def description(message: String): ScalaPactInteraction =
      new ScalaPactInteraction(message, None, None, ScalaPactRequest.default, ScalaPactResponse.default)
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

  val GET: HttpMethod     = HttpMethod.GET
  val POST: HttpMethod    = HttpMethod.POST
  val PUT: HttpMethod     = HttpMethod.PUT
  val DELETE: HttpMethod  = HttpMethod.DELETE
  val OPTIONS: HttpMethod = HttpMethod.OPTIONS
  val PATCH: HttpMethod   = HttpMethod.PATCH
  val CONNECT: HttpMethod = HttpMethod.CONNECT
  val TRACE: HttpMethod   = HttpMethod.TRACE
  val HEAD: HttpMethod    = HttpMethod.HEAD

  implicit def pactDescriptionOps(description: ScalaPactDescription): PactOps
}

sealed abstract class PactDescriptionOps(description: ScalaPactDescription) {
  /** Writes pacts described by this ScalaPactDescription to file without running any consumer tests
   */
  def writePactsToFile(implicit options: ScalaPactOptions, pactWriter: IPactWriter): Unit = {
    val pactDescription = finalise(options)
    ScalaPactContractWriter.writePactContracts(options.outputPath)(pactWriter)(pactDescription.withHeaderForSsl)
  }

  protected def finalise(implicit options: ScalaPactOptions): ScalaPactDescriptionFinal =
    ScalaPactDescriptionFinal(
      description.consumer,
      description.provider,
      description.sslContextName,
      description.interactions.map(i => i.finalise),
      options
    )
}

final class PactDescriptionImportOps(description: ScalaPactDescription) extends PactDescriptionOps(description) {
  def runConsumerTest[A](test: ScalaPactMockConfig => A)(implicit
                                                         options: ScalaPactOptions,
                                                         sslContextMap: SslContextMap,
                                                         pactReader: IPactReader,
                                                         pactWriter: IPactWriter,
                                                         httpClientBuilder: IScalaPactHttpClientBuilder,
                                                         pactStubber: IPactStubber
  ): A = {
    implicit val client: IScalaPactHttpClient =
      httpClientBuilder.build(2.seconds, description.sslContextName, 1)
    ScalaPactMock.runConsumerIntegrationTest(description.strict)(
      finalise
    )(test)
  }
}

final class PactDescriptionMixinOps(description: ScalaPactDescription) extends PactDescriptionOps(description) {
  def runConsumerTest[A](test: ScalaPactMockConfig => A)(implicit
                                                         options: ScalaPactOptions,
                                                         sslContextMap: SslContextMap,
                                                         pactReader: IPactReader,
                                                         pactWriter: IPactWriter,
                                                         httpClientBuilder: IScalaPactHttpClientBuilder,
                                                         pactStubber: IPactStubber
  ): A = {
    implicit val client: IScalaPactHttpClient =
      httpClientBuilder.build(2.seconds, description.sslContextName, 1)
    ScalaPactMock.runTestWithWarmedUpStubber(description.strict)(
      finalise
    )(test)
  }
}

sealed class ScalaPactForgerDslForImports extends ScalaPactForgerDsl[PactDescriptionImportOps] {
  implicit def pactDescriptionOps(description: ScalaPactDescription): PactDescriptionImportOps =
    new PactDescriptionImportOps(description)
}

trait ScalaPactForgerDslMixin extends ScalaPactForgerDsl[PactDescriptionMixinOps] {
  implicit def pactDescriptionOps(description: ScalaPactDescription): PactDescriptionMixinOps =
    new PactDescriptionMixinOps(description)
}

object ScalaPactForger extends ScalaPactForgerDslForImports

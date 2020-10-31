package com.itv.scalapact

import com.itv.scalapact.model.ScalaPactMatchingRule.{ScalaPactMatchingRuleArrayMinLength, ScalaPactMatchingRuleRegex, ScalaPactMatchingRuleType}
import com.itv.scalapact.model.{ScalaPactDescription, ScalaPactInteraction, ScalaPactMatchingRules, ScalaPactOptions, ScalaPactRequest, ScalaPactResponse}
import com.itv.scalapact.shared.http.HttpMethod

trait ScalaPactForgerDsl {
  implicit val options: ScalaPactOptions = ScalaPactOptions.DefaultOptions

  object forgePact extends ForgePactElements(strict = false)
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

object ScalaPactForger extends ScalaPactForgerDsl

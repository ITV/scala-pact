package com.itv.scalapact

import com.itv.scalapact.ScalaPactForger._
import com.itv.scalapact.model.ScalaPactMatchingRule.ScalaPactMatchingRuleArrayMinLength
import com.itv.scalapact.model._
import com.itv.scalapact.shared.MatchingRule
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers

class ScalaPactContractWriterSpec extends AnyFunSpec with Matchers {
  describe("Retrieving pact from description") {
    it("should produce pact with match: type for a minimum array rule") {
      val matchingRulesWithMinArray = List(ScalaPactMatchingRuleArrayMinLength("root", 10))
      val request = ScalaPactRequest(
        method = GET,
        path = "test path",
        query = None,
        headers = Map.empty,
        body = None,
        matchingRules = Some(matchingRulesWithMinArray)
      )

      val response = ScalaPactResponse(
        status = 200,
        headers = Map.empty,
        body = None,
        matchingRules = Some(matchingRulesWithMinArray)
      )

      val interactionWithMatchMinArray: ScalaPactInteractionFinal = ScalaPactInteractionFinal(
        description = "test description",
        providerState = None,
        sslContextName = None,
        request = request,
        response = response
      )
      val description = ScalaPactDescriptionFinal(
        consumer = "test consumer",
        provider = "test provider",
        serverSslContextName = None,
        interactions = List(interactionWithMatchMinArray),
        options = ScalaPactOptions(writePactFiles = false, "test output")
      )

      val pactFromDescription = ScalaPactContractWriter.producePactFromDescription(description)
      val resultInteraction   = pactFromDescription.interactions.head

      val requestMatchingRules = resultInteraction.request.matchingRules
      requestMatchingRules.get("root") shouldBe MatchingRule(Some("type"), None, Some(10))

      val responseMatchingRules = resultInteraction.response.matchingRules
      responseMatchingRules.get("root") shouldBe MatchingRule(Some("type"), None, Some(10))
    }
  }
}

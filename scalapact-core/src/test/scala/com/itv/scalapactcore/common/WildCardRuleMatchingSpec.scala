package com.itv.scalapactcore.common

import argonaut._
import Argonaut._
import com.itv.scalapactcore.MatchingRule
import com.itv.scalapactcore.common.matching.{MatchingRuleContext, RuleMatchFailure, RuleMatchSuccess, WildCardRuleMatching}
import org.scalatest.{FunSpec, Matchers}

class WildCardRuleMatchingSpec extends FunSpec with Matchers {

  describe("Matching rules containing wildcards") {

    it("should be able to match array elements") {

      val rule = "$.body.animals[*]"
      val path = ".animals[0]"

      WildCardRuleMatching.findMatchingRuleWithWildCards(path)(rule) shouldEqual true

    }

    it("should be able to match fields") {

      val rule = "$.body.animals.*"
      val path = ".animals.name"

      WildCardRuleMatching.findMatchingRuleWithWildCards(path)(rule) shouldEqual true

    }

    it("should be able to match array elements and fields") {

      val rule = "$.body.animals[*].*"
      val path = ".animals[0].name"

      WildCardRuleMatching.findMatchingRuleWithWildCards(path)(rule) shouldEqual true

    }

    it("should be able to find rules for arrays with possible sub elements") {

      val rule = "$.body.animals[*].*"
      val path = ".animals[0]"

      WildCardRuleMatching.findMatchingRuleWithWildCards(path)(rule) shouldEqual true

    }

    it("should be able to find a rule for something more complicated") {

      val rule = "$.body.animals[*].dogs[*].*"

      WildCardRuleMatching.findMatchingRuleWithWildCards(".animals[0].dogs[2]")(rule) shouldEqual true
      WildCardRuleMatching.findMatchingRuleWithWildCards(".animals[0].dogs[2].collie")(rule) shouldEqual true
      WildCardRuleMatching.findMatchingRuleWithWildCards(".animals[0].dogs[2].collies[1]")(rule) shouldEqual false
      WildCardRuleMatching.findMatchingRuleWithWildCards(".animals[0].dogs[2].collies[1].rover")(rule) shouldEqual false

    }

  }

  describe("Matching Json against wildcard rules") {

    it("Should pass in a simple value based case") {

      val ruleAndContext =
        MatchingRuleContext(
          path = ".animals[*]",
          rule = MatchingRule(
            Option("type"),
            regex = None,
            min = None
          )
        )

      val expectedArray: Json.JsonArray =
        """
          |[
          |  "Mary"
          |]
          |""".stripMargin
          .parseOption
          .flatMap(p => p.array)
          .get

      val receivedArray: Json.JsonArray =
        """
          |[
          |  "Mary","John"
          |]
          |""".stripMargin
          .parseOption
          .flatMap(p => p.array)
          .get

      val result = WildCardRuleMatching.arrayRuleMatchWithWildcards(".animals")(ruleAndContext)(expectedArray)(receivedArray)

      result shouldEqual RuleMatchSuccess
    }

    it("Should pass in a simple case") {

      val ruleAndContext =
        MatchingRuleContext(
          path = ".animals[*].*",
          rule = MatchingRule(
            Option("type"),
            regex = None,
            min = None
          )
        )

      val expectedArray: Json.JsonArray =
        """
          |[
          |  {
          |    "name" : "Mary"
          |  }
          |]
          |""".stripMargin
          .parseOption
          .flatMap(p => p.array)
          .get

      val receivedArray: Json.JsonArray =
        """
          |[
          |  {
          |    "name" : "Mary"
          |  },
          |  {
          |    "name" : "John"
          |  }
          |]
          |""".stripMargin
          .parseOption
          .flatMap(p => p.array)
          .get

      val result = WildCardRuleMatching.arrayRuleMatchWithWildcards(".animals")(ruleAndContext)(expectedArray)(receivedArray)

      result shouldEqual RuleMatchSuccess
    }

    it("Should fail in a simple case") {

      val ruleAndContext =
        MatchingRuleContext(
          path = ".animals[*].*",
          rule = MatchingRule(
            Option("type"),
            regex = None,
            min = None
          )
        )

      val expectedArray: Json.JsonArray =
        """
          |[
          |  {
          |    "name" : "Mary"
          |  }
          |]
          |""".stripMargin
          .parseOption
          .flatMap(p => p.array)
          .get

      val receivedArray: Json.JsonArray =
        """
          |[
          |  {
          |    "name" : "Mary"
          |  },
          |  {
          |    "name" : 1
          |  }
          |]
          |""".stripMargin
          .parseOption
          .flatMap(p => p.array)
          .get

      val result = WildCardRuleMatching.arrayRuleMatchWithWildcards(".animals")(ruleAndContext)(expectedArray)(receivedArray)

      result shouldEqual RuleMatchFailure
    }

    it("Should succeed in a nested case") {

      val ruleAndContext =
        MatchingRuleContext(
          path = ".animals[*].dogs[*].*",
          rule = MatchingRule(
            Option("type"),
            regex = None,
            min = None
          )
        )

      val expectedArray: Json.JsonArray =
        """
          |[
          |  {
          |    "name" : "Mary",
          |    "dogs" : [
          |      {
          |        "breed" : "collie"
          |      }
          |    ]
          |  }
          |]
          |""".stripMargin
          .parseOption
          .flatMap(p => p.array)
          .get

      val receivedArray: Json.JsonArray =
        """
          |[
          |  {
          |    "name" : "Mary",
          |    "dogs" : [
          |      {
          |        "breed" : "collie"
          |      }
          |    ]
          |  },
          |  {
          |    "name" : "John",
          |    "dogs" : [
          |      {
          |        "breed" : "collie"
          |      },
          |      {
          |        "breed" : "GSD"
          |      }
          |    ]
          |  }
          |]
          |""".stripMargin
          .parseOption
          .flatMap(p => p.array)
          .get

      val result = WildCardRuleMatching.arrayRuleMatchWithWildcards(".animals")(ruleAndContext)(expectedArray)(receivedArray)

      result shouldEqual RuleMatchSuccess
    }

  }

}

package com.itv.scalapactcore.common.matchir

import com.itv.scalapactcore.MatchingRule
import org.scalatest.{FunSpec, Matchers}

class IrNodeRuleSpec extends FunSpec with Matchers {

  describe("creating a rule set") {

    it("should be able to convert pact matching rules into IrNodeRules") {

      val pactRules: Option[Map[String, MatchingRule]] = Option {
        Map(
          ".fish" -> MatchingRule(Some("type"), None, None),
          ".fish.breed" -> MatchingRule(Some("regex"), Some("cod|haddock"), None),
          ".fish.fins" -> MatchingRule(Some("min"), None, Some(1))
        )
      }

      val expected: IrNodeMatchingRules =
        IrNodeMatchingRules(
          IrNodeTypeRule(IrNodePathEmpty <~ "fish"),
          IrNodeRegexRule("cod|haddock", IrNodePathEmpty <~ "fish" <~ "breed"),
          IrNodeMinArrayLengthRule(1, IrNodePathEmpty <~ "fish" <~ "fins")
        )

      IrNodeMatchingRules.fromPactRules(pactRules) shouldEqual expected

    }

  }

  describe("Validating a node using rules") {

    it("should be able to compare node types") {
      pending
    }

    it("should be able to compare node primitive types") {
      pending
    }

    it("should not validate a node using regex") {
      pending
    }

    it("should be able to validate a node primitive using regex") {
      pending
    }

    it("should be able to check an array node is of minimum length") {
      pending
    }

    it("should not attempt to check the array length of a primitive") {
      pending
    }

  }

}

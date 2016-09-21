package com.itv.scalapactcore.common

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

  }

}

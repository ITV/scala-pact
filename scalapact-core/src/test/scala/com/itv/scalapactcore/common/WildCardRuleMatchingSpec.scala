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

}

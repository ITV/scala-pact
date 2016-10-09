package com.itv.scalapactcore.common

import com.itv.scalapactcore.common.matching.PlainTextEquality
import org.scalatest.{FunSpec, Matchers}

class PlainTextEqualitySpec extends FunSpec with Matchers {

  describe("checking for plain text equality") {

    it("Should match equal strings") {

      PlainTextEquality.check("fish", "fish")
      PlainTextEquality.check("fish", " fish")
      PlainTextEquality.check("fish", "fish    ")
      PlainTextEquality.check("fish", "  fish    ")
      PlainTextEquality.check(" fish", "fish")

    }

  }

}

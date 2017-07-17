package com.itv.scalapactcore.common.matchir

import org.scalatest.{FunSpec, Matchers}

class IrEqualitySpec extends FunSpec with Matchers {

  describe("strictly matching structures") {

    it("should check simple equality") {

      val a = IrNode("fish", IrNode("breed", IrStringNode("cod")))
      val b = IrNode("fish", IrNode("breed", IrStringNode("haddock")))

      a shouldEqual a
      a shouldNot be(b)

    }

  }

}

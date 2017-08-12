package com.itv.scalapactcore.common.matchir

import org.scalatest.{FunSpec, Matchers}

class IrNodePathSpec extends FunSpec with Matchers {

  describe("Build IrNodePath's") {

    it("should be able to represent an empty path") {
      val path: IrNodePath = IrNodePathEmpty

      path.renderAsString shouldEqual "."
    }

    it("should be able to represent going down through object fields") {
      val path = IrNodePathEmpty <~ "fish" <~ "chips"

      path.renderAsString shouldEqual ".fish.chips"
    }

    it("should be able to represent going down into an array") {
      val path = IrNodePathEmpty <~ "fish" <~ "chips" <~ 2 <~ "ketchup"

      path.renderAsString shouldEqual ".fish.chips[2].ketchup"
    }

    it("should be able to represent any element in an array") {
      val path = IrNodePathEmpty <~ "fish" <~ "chips" <~ "*" <~ "ketchup"

      path.renderAsString shouldEqual ".fish.chips[*].ketchup"
    }

  }

  describe("converting JsonPath to IrNodePath") {
    pending
  }

  describe("converting XmlPath to IrNodePath") {
    pending
  }

  describe("comparing two IrNodePaths") {

    it("should be able to test that equal paths are equal") {

      IrNodePathEmpty === IrNodePathEmpty shouldEqual true
      IrNodePathEmpty === (IrNodePathEmpty <~ "fish" <~ "chips" <~ 2 <~ "ketchup") shouldEqual false
      (IrNodePathEmpty <~ "fish" <~ "chips") === (IrNodePathEmpty <~ "fish" <~ "chips") shouldEqual true
      (IrNodePathEmpty <~ "fish" <~ "chips" <~ 2 <~ "ketchup") === (IrNodePathEmpty <~ "fish" <~ "chips" <~ 2 <~ "ketchup") shouldEqual true

    }

    it("should be able to test that references to 'any element' are accepted") {

      (IrNodePathEmpty <~ "fish" <~ "chips" <~ "*" <~ "ketchup") === (IrNodePathEmpty <~ "fish" <~ "chips" <~ "*" <~ "ketchup") shouldEqual true
      (IrNodePathEmpty <~ "fish" <~ "chips" <~ "*" <~ "ketchup") === (IrNodePathEmpty <~ "fish" <~ "chips" <~ 2 <~ "ketchup") shouldEqual true
      (IrNodePathEmpty <~ "fish" <~ "chips" <~ 1 <~ "ketchup") === (IrNodePathEmpty <~ "fish" <~ "chips" <~ "*" <~ "ketchup") shouldEqual true

    }

  }

}

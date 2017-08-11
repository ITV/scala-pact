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

  }

  describe("converting XmlPath to IrNodePath") {
    pending
  }

  describe("converting JsonPath to IrNodePath") {
    pending
  }

  describe("comparing two IrNodePaths") {
    pending
  }

}

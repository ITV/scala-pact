package com.itv.scalapact.shared.utils

import org.scalatest.{FunSpec, Matchers}

class HelpersSpec extends FunSpec with Matchers {

  describe("Pairing things") {

    it("should be able to pair a list into a Map") {

      Helpers.pair(List(1, 2, 3, 4)) shouldEqual Map(1 -> 2, 3 -> 4)
      Helpers.pair(List("a", "b", "c")) shouldEqual Map("a" -> "b")
    }

    it("should be able to pair a list into a list of tuples") {

      Helpers.pairTuples(List(1, 2, 3, 4)) shouldEqual List((1, 2), (3, 4))
      Helpers.pairTuples(List("a", "b", "c")) shouldEqual List(("a", "b"))

    }

    it("should parse various date strings") {
      Helpers.safeStringToDateTime("2020-08-06").isDefined shouldBe true
      Helpers.safeStringToDateTime("2020-08-06T10:30:30").isDefined shouldBe true
      Helpers.safeStringToDateTime("2020-08-06T10:30:30+01:00").isDefined shouldBe true
    }

  }

}

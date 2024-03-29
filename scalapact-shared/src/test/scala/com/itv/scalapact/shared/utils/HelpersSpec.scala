package com.itv.scalapact.shared.utils

import java.time.{OffsetDateTime, ZoneOffset}

import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers

class HelpersSpec extends AnyFunSpec with Matchers {

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
      Helpers.safeStringToDateTime("2020-08-06") shouldBe
        Some(OffsetDateTime.of(2020, 8, 6, 0, 0, 0, 0, ZoneOffset.UTC))
      Helpers.safeStringToDateTime("2020-08-06T10:30:30") shouldBe
        Some(OffsetDateTime.of(2020, 8, 6, 10, 30, 30, 0, ZoneOffset.UTC))
      Helpers.safeStringToDateTime("2020-08-06T10:30:30+01:00") shouldBe
        Some(OffsetDateTime.of(2020, 8, 6, 10, 30, 30, 0, ZoneOffset.ofHours(1)))
    }

  }

}

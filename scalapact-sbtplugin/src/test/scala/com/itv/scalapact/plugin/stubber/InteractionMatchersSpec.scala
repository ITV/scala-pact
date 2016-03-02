package com.itv.scalapact.plugin.stubber

import org.scalatest.{Matchers, FunSpec}
import scala.language.implicitConversions

class InteractionMatchersSpec extends FunSpec with Matchers {


  implicit def toOption[A](thing: A): Option[A] = Option(thing)

  import InteractionMatchers._

  describe("Matching status codes") {

    it("should be able to match status codes") {

      matchStatusCodes(200)(200) shouldEqual true
      matchStatusCodes(200)(503) shouldEqual false

    }

  }

  describe("Matching methods") {

    it("should be able to match methods") {

      matchMethods("GET")("GET") shouldEqual true
      matchMethods("GET")("POST") shouldEqual false
      matchMethods("GET")("nonsense") shouldEqual false

    }

  }

  describe("Matching headers") {

    it("should be able to match simple headers") {

      val expected = Map("fish" -> "chips")
      val received = Map("fish" -> "chips")

      matchHeaders(expected)(received) shouldEqual true

    }

    it("should be able to spot mismatched headers") {

      val expected = Map("fish" -> "chips")
      val received = Map("fish" -> "peas")

      matchHeaders(expected)(received) shouldEqual false

    }

    it("should be able to find the expected subset in a collection of headers") {

      val expected = Map("fish" -> "chips", "mammal" -> "bear")
      val received = Map("fish" -> "chips", "mammal" -> "bear", "rock" -> "sandstone", "metal" -> "steel")

      matchHeaders(expected)(received) shouldEqual true

    }

  }

  describe("Matching paths") {

    it("should be able to match paths") {

      val expected = "/foo/bar/hello?id=abc123&name=joey&job=dentist&hobby=skiing"

      matchPaths(expected)(expected) shouldEqual true
      matchPaths(expected)("/foo/bar/hello?hobby=skiing&name=joey&id=abc123&job=dentist") shouldEqual true
      matchPaths(expected)("/foo/bar/hello?hobby=skiing") shouldEqual false
      matchPaths("/foo/bar/hello")("/foo/bar/hello?hobby=skiing") shouldEqual false

    }

  }

}

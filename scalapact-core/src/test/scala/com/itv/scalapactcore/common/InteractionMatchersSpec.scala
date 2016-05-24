package com.itv.scalapactcore.common

import com.itv.scalapactcore.MatchingRule
import org.scalatest.{FunSpec, Matchers}

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

      matchHeaders(None)(expected)(received) shouldEqual true

    }

    it("should be able to spot mismatched headers") {

      val expected = Map("fish" -> "chips")
      val received = Map("fish" -> "peas")

      matchHeaders(None)(expected)(received) shouldEqual false

    }

    it("should be able to find the expected subset in a collection of headers") {

      val expected = Map("fish" -> "chips", "mammal" -> "bear")
      val received = Map("fish" -> "chips", "mammal" -> "bear", "rock" -> "sandstone", "metal" -> "steel")

      matchHeaders(None)(expected)(received) shouldEqual true

    }

    it("should be able to handle a more complex case") {

      val expected = None
      val received = Option(
          Map(
            "Upgrade-Insecure-Requests" -> "1",
            "Connection" -> "keep-alive",
            "Accept" -> "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8",
            "Cache-Control" -> "max-age=0",
            "Accept-Language" -> "en-US,en;q=0.8",
            "Accept-Encoding" -> "gzip",
            "deflate" -> "",
            "sdch" -> "",
            "User-Agent" -> "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_10_5) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/48.0.2564.116 Safari/537.36",
            "Host" -> "localhost:1234"
          )
        )

      matchHeaders(None)(expected)(received) shouldEqual true

    }

    it("should be able to handle a more complex case where it needs to match") {

      val expected = Option(
        Map(
          "Accept" -> "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8",
          "Host" -> "localhost:1234"
        )
      )
      val received = Option(
        Map(
          "Upgrade-Insecure-Requests" -> "1",
          "Connection" -> "keep-alive",
          "Accept" -> "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8",
          "Cache-Control" -> "max-age=0",
          "Accept-Language" -> "en-US,en;q=0.8",
          "Accept-Encoding" -> "gzip",
          "deflate" -> "",
          "sdch" -> "",
          "User-Agent" -> "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_10_5) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/48.0.2564.116 Safari/537.36",
          "Host" -> "localhost:1234"
        )
      )

      matchHeaders(None)(expected)(received) shouldEqual true

    }

    it("should be able to use regex to match headers") {
      val expected = Option(
        Map(
          "fish" -> "chips"
        )
      )
      val received = Option(
        Map(
          "fish" -> "peas"
        )
      )

      matchHeaders(Option(Map("$.headers.fish" -> MatchingRule("regex", "\\w+"))))(expected)(received) shouldEqual true
    }

    it("should be able to use regex to match more realistic custom headers") {
      val expected = Option(
        Map(
          "Accept" -> "application/vnd.itv.oas.variant.v1+json",
          "X-Trace-Id" -> "7656163a-eefb-49f8-9fac-b20b33dfb51b"
        )
      )
      val received = Option(
        Map(
          "Accept" -> "application/vnd.itv.oas.variant.v1+json",
          "X-Trace-Id" -> "7656163a-eefb-49f8-9fac-b20b33dfb51B" // Changed to captial B
        )
      )

      matchHeaders(Option(Map("$.headers.X-Trace-Id" -> MatchingRule("regex", "^.{0,38}$"))))(expected)(received) shouldEqual true
      matchHeaders(Option(Map("$.headers.X-Trace-Id" -> MatchingRule("regex", "^fish"))))(expected)(received) shouldEqual false
    }

    it("should be able to use regex to match some headers out of sequence") {
      val expected = Option(
        Map(
          "sauce" -> "ketchup",
          "fish" -> "chips"
        )
      )
      val received = Option(
        Map(
          "fish" -> "peas",
          "sauce" -> "ketchup"
        )
      )

      matchHeaders(Option(Map("$.headers.fish" -> MatchingRule("regex", "\\w+"))))(expected)(received) shouldEqual true
    }

  }

  describe("Matching paths") {

    it("should be able to match paths") {

      val expected = "/foo/bar/hello?id=abc123&name=joey&job=dentist&hobby=skiing"

      matchPaths(expected)(None)(expected)(None) shouldEqual true
      matchPaths(expected)(None)("/foo/bar/hello?hobby=skiing&name=joey&id=abc123&job=dentist")(None) shouldEqual true
      matchPaths(expected)(None)("/foo/bar/hello?hobby=skiing&name=joey")("id=abc123&job=dentist") shouldEqual true
      matchPaths(expected)(None)("/foo/bar/hello?hobby=skiing")(None) shouldEqual false
      matchPaths("/foo/bar/hello")(None)("/foo/bar/hello?hobby=skiing")(None) shouldEqual true // forgiving in what you receive...

    }

  }

  describe("Matching bodies") {

    it("should be able to match plain text bodies") {

      val expected = "hello there!"

      matchBodies(expected)(expected) shouldEqual true
      matchBodies(expected)("Yo ho!") shouldEqual false

    }

    it("should be able to handle missing bodies and no expectation of bodies") {

      withClue("None expected, none received") {
        matchBodies(None)(None) shouldEqual true
      }

      withClue("Some expected, none received") {
        matchBodies(Some("hello"))(None) shouldEqual false
      }

      // Forgiving about what we receive
      withClue("None expected, some received") {
        matchBodies(None)(Some("hello")) shouldEqual true
      }
      withClue("Some expected, some received") {
        matchBodies(Some("hello"))(Some("hello")) shouldEqual true
      }

    }

    it("should be able to match json bodies") {

      val expected =
        """
          |{
          |  "id":1234,
          |  "name":"joe",
          |  "hobbies": [
          |    "skiing",
          |    "fishing", "golf"
          |  ]
          |}
        """.stripMargin

      val received =
        """
          |{
          |  "id":1234,
          |  "name":"joe",
          |  "hobbies": [
          |    "skiing",
          |    "fishing","golf"
          |  ]
          |}
        """.stripMargin

      val received2 =
        """
          |{
          |  "id":1234,
          |  "hobbies": [
          |    "skiing",
          |    "fishing",
          |    "golf"
          |  ],
          |  "name":"joe"
          |}
        """.stripMargin

      val received3 =
        """
          |{
          |  "id":1234,
          |  "hobbies": [
          |    "fishing",
          |    "skiing",
          |    "golf"
          |  ],
          |  "name":"joe"
          |}
        """.stripMargin

      val expected2 =
        """
          |{
          |  "id":1234,
          |  "hobbies": [
          |   {
          |     "type":"fish"
          |   },
          |   {
          |     "type":"mammal"
          |   }
          |  ],
          |  "name":"joe"
          |}
        """.stripMargin

      val received4 =
        """
          |{
          |  "id":1234,
          |  "hobbies": [
          |   {
          |     "type":"mammal"
          |   },
          |   {
          |     "type":"fish"
          |   }
          |  ],
          |  "name":"joe"
          |}
        """.stripMargin

      withClue("Same json no hal") {
        matchBodies(expected)(expected) shouldEqual true
      }

      withClue("Same json + hal") {
        matchBodies(expected)(expected) shouldEqual true
      }

      withClue("Expected compared to received") {
        matchBodies(expected)(received) shouldEqual true
      }

      withClue("Expected compared to a received object with the fields in a different order") {
        //matchBodies(Option(Map("Content-Type" -> "application/json")))(expected)(received2) shouldEqual true
      }

      withClue("Expected compared to a received object with the array in a different order") {
        //matchBodies(Option(Map("Content-Type" -> "application/json")))(expected)(received3) shouldEqual false
      }

      withClue("Expected compared to a received object with the object array in a different order") {
        //matchBodies(Option(Map("Content-Type" -> "application/json")))(expected2)(received4) shouldEqual false
      }

    }

  }

}

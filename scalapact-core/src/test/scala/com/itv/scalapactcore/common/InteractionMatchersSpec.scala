package com.itv.scalapactcore.common

import com.itv.scalapact.shared.pact.PactReader
import com.itv.scalapact.shared.{MatchingRule, Pact}
import com.itv.scalapactcore.common.matching.BodyMatching._
import com.itv.scalapactcore.common.matching.HeaderMatching._
import com.itv.scalapactcore.common.matching.{InteractionMatchers, MatchOutcomeFailed, MatchOutcomeSuccess}
import com.itv.scalapactcore.common.matching.InteractionMatchers.OutcomeAndInteraction
import com.itv.scalapactcore.common.matching.MethodMatching._
import com.itv.scalapactcore.common.matching.PathMatching._
import com.itv.scalapactcore.common.matching.StatusMatching._
import com.itv.scalapactcore.common.matchir.IrNodeMatchingRules
import org.scalatest.{FunSpec, Matchers}

import scala.language.implicitConversions

class InteractionMatchersSpec extends FunSpec with Matchers {

  implicit def toOption[A](thing: A): Option[A] = Option(thing)

  describe("Matching status codes") {

    it("should be able to match status codes") {

      matchStatusCodes(200, 200).isSuccess shouldEqual true
      matchStatusCodes(200, 503).isSuccess shouldEqual false

    }

  }

  describe("Matching methods") {

    it("should be able to match methods") {

      matchMethods("GET", "GET").isSuccess shouldEqual true
      matchMethods("GET", "POST").isSuccess shouldEqual false
      matchMethods("GET", "nonsense").isSuccess shouldEqual false

    }

  }

  describe("Matching headers") {

    it("should be able to match simple headers") {

      val expected = Map("fish" -> "chips")
      val received = Map("fish" -> "chips")

      matchHeaders(None, expected, received).isSuccess shouldEqual true

    }

    it("should be able to spot mismatched headers") {

      val expected = Map("fish" -> "chips")
      val received = Map("fish" -> "peas")

      matchHeaders(None, expected, received).isSuccess shouldEqual false

    }

    it("should be able to find the expected subset in a collection of headers") {

      val expected = Map("fish" -> "chips", "mammal" -> "bear")
      val received = Map("fish" -> "chips", "mammal" -> "bear", "rock" -> "sandstone", "metal" -> "steel")

      matchHeaders(None, expected, received).isSuccess shouldEqual true

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

      matchHeaders(None, expected, received).isSuccess shouldEqual true

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

      matchHeaders(None, expected, received) match {
        case s @ MatchOutcomeSuccess =>
          s.isSuccess shouldEqual true

        case f: MatchOutcomeFailed =>
          fail(f.renderDifferences)
      }

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

      matchHeaders(Option(Map("$.headers.fish" -> MatchingRule("regex", "\\w+", min = None))), expected, received).isSuccess shouldEqual true
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

      matchHeaders(Option(Map("$.headers.X-Trace-Id" -> MatchingRule("regex", "^.{0,38}$", min = None))), expected, received).isSuccess shouldEqual true
      matchHeaders(Option(Map("$.headers.X-Trace-Id" -> MatchingRule("regex", "^fish", min = None))), expected, received).isSuccess shouldEqual false
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

      matchHeaders(Option(Map("$.headers.fish" -> MatchingRule("regex", "\\w+", min = None))), expected, received).isSuccess shouldEqual true
    }

  }

  describe("Matching paths") {

    it("should be able to match paths") {

      val expected = "/foo/bar/hello?id=abc123&name=joey&job=dentist&hobby=skiing"

      matchPaths(PathAndQuery(expected, None), PathAndQuery(expected, None)).isSuccess shouldEqual true
      matchPaths(PathAndQuery(expected, None), PathAndQuery("/foo/bar/hello?hobby=skiing&name=joey&id=abc123&job=dentist", None)).isSuccess shouldEqual true
      matchPaths(PathAndQuery(expected, None), PathAndQuery("/foo/bar/hello?hobby=skiing&name=joey", "id=abc123&job=dentist")).isSuccess shouldEqual true
      matchPaths(PathAndQuery(expected, None), PathAndQuery("/foo/bar/hello?hobby=skiing", None)).isSuccess shouldEqual false
      matchPaths(PathAndQuery("/foo/bar/hello", None), PathAndQuery("/foo/bar/hello?hobby=skiing", None)).isSuccess shouldEqual true // forgiving in what you receive...

    }

  }

  describe("Matching bodies") {

    it("should be able to match plain text bodies") {

      val expected = "hello there!"

      matchBodies(None, expected, expected)(IrNodeMatchingRules.empty).isSuccess shouldEqual true
      matchBodies(None, expected, "Yo ho!")(IrNodeMatchingRules.empty).isSuccess shouldEqual false

    }

    it("should be able to handle missing bodies and no expectation of bodies") {

      withClue("None expected, none received") {
        matchBodies(None, None, None)(IrNodeMatchingRules.empty).isSuccess shouldEqual true
      }

      withClue("Some expected, none received") {
        matchBodies(None, Some("hello"), None)(IrNodeMatchingRules.empty).isSuccess shouldEqual false
      }

      // Forgiving about what we receive
      withClue("None expected, some received") {
        matchBodies(None, None, Some("hello"))(IrNodeMatchingRules.empty).isSuccess shouldEqual true
      }
      withClue("Some expected, some received") {
        matchBodies(None, Some("hello"), Some("hello"))(IrNodeMatchingRules.empty).isSuccess shouldEqual true
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

      withClue("Same json no hal") {
        matchBodies(None, expected, expected)(IrNodeMatchingRules.empty).isSuccess shouldEqual true
      }

      withClue("Same json + hal") {
        matchBodies(None, expected, expected)(IrNodeMatchingRules.empty).isSuccess shouldEqual true
      }

      withClue("Expected compared to received") {
        matchBodies(None, expected, received)(IrNodeMatchingRules.empty).isSuccess shouldEqual true
      }

    }

    it("should be able to match json bodies with rules") {

      val expected =
        """
          |{
          |  "name":"joe"
          |}
        """.stripMargin

      val received =
        """
          |{
          |  "name":"eirik"
          |}
        """.stripMargin

      val rules: Option[Map[String, MatchingRule]] = Option(
        Map(
          "$.body.name" -> MatchingRule(Option("regex"), Option("\\w+"), None)
        )
      )

      IrNodeMatchingRules.fromPactRules(rules) match {
        case Left(e) =>
          fail(e)

        case Right(r) =>
          withClue("Didn't match json body with rule") {
            matchBodies(None, expected, received)(r).isSuccess shouldEqual true
          }
      }


    }

    it("should be able to match xml bodies") {

      val expected1 =
        <fish-supper>
          <fish>cod</fish>
          <chips>obviously</chips>
          <sauce>ketchup</sauce>
        </fish-supper>

      val received1 =
        <fish-supper>
          <fish>cod</fish>
          <chips>obviously</chips>
          <sauce>ketchup</sauce>
        </fish-supper>

      withClue("Same xml") {
        matchBodies(None, expected1.toString(), received1.toString())(IrNodeMatchingRules.empty).isSuccess shouldEqual true
      }

      val expected2 =
        <fish-supper>
          <fish>cod</fish>
          <chips>obviously</chips>
          <sauce>ketchup</sauce>
        </fish-supper>

      val received2 =
        <fish-supper>
          <fish>cod</fish>
          <chips>not too many...</chips>
          <sauce>ketchup</sauce>
        </fish-supper>

      withClue("Different xml") {
        matchBodies(None, expected2.toString(), received2.toString())(IrNodeMatchingRules.empty).isSuccess shouldEqual false
      }

      val expected3 =
        <fish-supper>
          <fish sustainable="true">cod</fish>
          <chips>obviously</chips>
          <sauce>ketchup</sauce>
        </fish-supper>

      val received3 =
        <fish-supper>
          <fish sustainable="true" oceanic="true">cod</fish>
          <chips>obviously</chips>
          <sauce>ketchup</sauce>
          <pickle>beetroot</pickle>
          <gravy/>
        </fish-supper>

      withClue("Received xml with additional fields and attributes") {
        matchBodies(None, expected3.toString(), received3.toString())(IrNodeMatchingRules.empty).isSuccess shouldEqual true
      }

      val expected4 =
        <fish-supper>
          <fish sustainable="true" oceanic="true">cod</fish>
          <chips>obviously</chips>
          <sauce>ketchup</sauce>
          <pickle>beetroot</pickle>
          <gravy/>
        </fish-supper>

      val received4 =
        <fish-supper>
          <fish sustainable="true">cod</fish>
          <chips>obviously</chips>
          <sauce>ketchup</sauce>
        </fish-supper>

      withClue("Received xml with missing fields and attributes") {
        matchBodies(None, expected4.toString(), received4.toString())(IrNodeMatchingRules.empty).isSuccess shouldEqual false
      }

    }

  }

  describe("Drift AKA Choosing the closest failed match") {

    it("should be able to pick the matching response") {

      val pactExpected: Pact =
        makePact(
          makeInteraction(
            "A",
            "200",
            """{"message": "Hello"}"""
          ),
          makeInteraction(
            "B",
            "404",
            """{"message": "Hello"}"""
          )
        )

      val pactActual: Pact =
        makePact(
          makeInteraction(
            "",
            "200",
            """{"message": "Hello"}"""
          )
        )

      val res: OutcomeAndInteraction = InteractionMatchers.matchOrFindClosestResponse(true, pactExpected.interactions, pactActual.interactions.head.response).get

      res.closestMatchingInteraction.description shouldEqual "A"
      res.outcome shouldBe MatchOutcomeSuccess

    }

    it("should pick the last response were there is an equal amount of drift.") {

      val pactExpected: Pact =
        makePact(
          makeInteraction(
            "A",
            "500",
            """{"message": "Hello"}"""
          ),
          makeInteraction(
            "B",
            "404",
            """{"message": "Hello"}"""
          )
        )

      val pactActual: Pact =
        makePact(
          makeInteraction(
            "",
            "200",
            """{"message": "Hello"}"""
          )
        )

      val res: OutcomeAndInteraction = InteractionMatchers.matchOrFindClosestResponse(true, pactExpected.interactions, pactActual.interactions.head.response).get

      res.closestMatchingInteraction.description shouldEqual "B"

      res.outcome match {
        case MatchOutcomeSuccess =>
          fail("Should not have matched")

        case f @ MatchOutcomeFailed(_, drift) =>
          f.errorCount shouldEqual 1
          drift shouldEqual 50
      }

    }

    it("should pick the closest match response were there is an unequal amount of drift. (case 1)") {

      val pactExpected: Pact =
        makePact(
          makeInteraction(
            "A",
            "500",
            """{"message": "Hello"}"""
          ),
          makeInteraction(
            "B",
            "404",
            """{"message": "Hello2"}"""
          )
        )

      val pactActual: Pact =
        makePact(
          makeInteraction(
            "",
            "200",
            """{"message": "Hello"}"""
          )
        )

      val res: OutcomeAndInteraction = InteractionMatchers.matchOrFindClosestResponse(true, pactExpected.interactions, pactActual.interactions.head.response).get

      res.closestMatchingInteraction.description shouldEqual "A"

      res.outcome match {
        case MatchOutcomeSuccess =>
          fail("Should not have matched")

        case f @ MatchOutcomeFailed(_, drift) =>
          f.errorCount shouldEqual 1
          drift shouldEqual 50
      }

    }

    it("should pick the closest match response were there is an unequal amount of drift. (case 2)") {

      val pactExpected: Pact =
        makePact(
          makeInteraction(
            "A",
            "500",
            """{"message": "Hello"}"""
          ),
          makeInteraction(
            "B",
            "200",
            """{"message": "Hello2"}"""
          )
        )

      val pactActual: Pact =
        makePact(
          makeInteraction(
            "",
            "200",
            """{"message": "Hello"}"""
          )
        )

      val res: OutcomeAndInteraction = InteractionMatchers.matchOrFindClosestResponse(true, pactExpected.interactions, pactActual.interactions.head.response).get

      res.closestMatchingInteraction.description shouldEqual "B"

      res.outcome match {
        case MatchOutcomeSuccess =>
          fail("Should not have matched")

        case f @ MatchOutcomeFailed(_, drift) =>
          f.errorCount shouldEqual 1
          drift shouldEqual 1
      }

    }

    def makeInteraction(description: String, status: String, body: String): String = {
        s"""
          |    {
          |      "description" : "$description",
          |      "request" : {
          |        "method" : "GET",
          |        "path" : "/"
          |      },
          |      "response" : {
          |        "status" : $status,
          |        "body" : $body,
          |        "matchingRules": {}
          |      }
          |    }
        """.stripMargin
    }

    def makePact(interactions: String*): Pact = {
      val json: String =
        s"""{
          |  "provider" : {
          |    "name" : "provider"
          |  },
          |  "consumer" : {
          |    "name" : "consumer"
          |  },
          |  "interactions" : [${interactions.toList.mkString(",")}]
          |}""".stripMargin

      PactReader.jsonStringToPact(json) match {
        case Right(p) =>
          p

        case Left(s) =>
          throw new Exception(s)
      }
    }

  }

}

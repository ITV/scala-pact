package com.itv.scalapactcore.common.matchir

import com.itv.scalapactcore.common.matchir.PactPathParseResult.{PactPathParseFailure, PactPathParseSuccess}
import org.scalatest.{FunSpec, Matchers}

import scala.language.postfixOps

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

    it("should be able to represent a path to an xml attribute") {
      val path = IrNodePathEmpty <~ "fish" <~ "chips" <~ "*" <~ "ketchup" <@ "applied"

      path.renderAsString shouldEqual ".fish.chips[*].ketchup['@applied']"
    }

    it("should be able to represent a path to an xml text element") {
      val path = IrNodePathEmpty <~ "fish" <~ "chips" <~ "*" <~ "ketchup" text

      path.renderAsString shouldEqual ".fish.chips[*].ketchup['#text']"
    }

  }

  describe("path traversal") {

    it("should be able to return a path to it's parent") {

      (IrNodePathEmpty <~ "fish" <~ "chips").parent === (IrNodePathEmpty <~ "fish") shouldEqual true
      (IrNodePathEmpty <~ "fish" <~ 2 <~ "chips").parent ===  (IrNodePathEmpty <~ "fish" <~ 2) shouldEqual true
      (IrNodePathEmpty <~ "fish" <~ "chips" <~ 2).parent ===  (IrNodePathEmpty <~ "fish" <~ "chips") shouldEqual true
      (IrNodePathEmpty <~ "fish" <~ "*" <~ "chips").parent ===  (IrNodePathEmpty <~ "fish" <~ "*") shouldEqual true
      (IrNodePathEmpty <~ "fish" <~ "chips" <~ "*").parent ===  (IrNodePathEmpty <~ "fish" <~ "chips") shouldEqual true
      (IrNodePathEmpty <~ "fish" <~ "chips" <@ "ketchup").parent ===  (IrNodePathEmpty <~ "fish" <~ "chips") shouldEqual true
      (IrNodePathEmpty <~ "fish" <~ "chips" text).parent ===  (IrNodePathEmpty <~ "fish" <~ "chips") shouldEqual true

    }

  }

  describe("path rewriting") {

    it("should be able to split a path into segments") {

      val path = IrNodePathEmpty <~ "pond" <~ "fish" <~ "*" <~ "breed" <~ "cod"

      val expected = List(
        IrNodePathEmpty <~ "pond",
        IrNodePathEmpty <~ "fish" <~ "*",
        IrNodePathEmpty <~ "breed",
        IrNodePathEmpty <~ "cod"
      )

      path.split shouldEqual expected

    }

    it("should be able to combine segments into a path") {

      val path = IrNodePathEmpty <~ "pond" <~ "fish" <~ "*" <~ "breed" <~ "cod"

      val segments = List(
        IrNodePathEmpty <~ "pond",
        IrNodePathEmpty <~ "fish" <~ "*",
        IrNodePathEmpty <~ "breed",
        IrNodePathEmpty <~ "cod"
      )

      IrNodePath.combine(segments) === path shouldEqual true

    }

    it("should be able to invert a path") {

      val actual = (IrNodePathEmpty <~ "fish" <~ "*" <~ "breed" <~ "cod" <~ "colour").invert

      val expected = IrNodePathEmpty <~ "colour" <~ "cod" <~ "breed" <~ "fish" <~ "*"

      actual === expected shouldEqual true

    }

    it("should be able to append/re-parent one path on another") {

      val expected = IrNodePathEmpty <~ "fish" <~ "*" <~ "breed" <~ "cod" <~ "colour"

      val actual = (IrNodePathEmpty <~ "fish" <~ "*" <~ "breed") ++ (IrNodePathEmpty <~ "cod" <~ "colour")

      actual === expected shouldEqual true

    }

    it("should be able to return a list of paths with backwards indexes") {

      val expected: List[IrNodePath] = List(
        IrNodePathEmpty <~ "fish" <~ "chips" <~ 2 <~ "sauce" <~ 0,
        IrNodePathEmpty <~ "fish" <~ 0 <~ "chips" <~ 2 <~ "sauce",
        IrNodePathEmpty <~ "fish" <~ 0 <~ "chips" <~ 2 <~ "sauce" <~ 0
      )

      val path = IrNodePathEmpty <~ "fish" <~ "chips" <~ 2 <~ "sauce"

      val actual = path.withIndexes

      expected.foreach { p =>
        actual.exists(_ === p) shouldEqual true
      }

    }

  }

  describe("converting IrNodePath to and from PactPath") {

    it("should strip off '$.body'") {
      val jsonPath = "$.body.foo"

      val expected = IrNodePathEmpty <~ "foo"

      PactPath.fromPactPath(jsonPath) match {
        case PactPathParseSuccess(irNodePath) =>
          irNodePath === expected shouldEqual true

        case e: PactPathParseFailure =>
          fail(e.errorString)
      }
    }

    it("should strip off '$.headers'") {
      val jsonPath = "$.headers.foo"

      val expected = IrNodePathEmpty <~ "foo"

      PactPath.fromPactPath(jsonPath) match {
        case PactPathParseSuccess(irNodePath) =>
          irNodePath === expected shouldEqual true

        case e: PactPathParseFailure =>
          fail(e.errorString)
      }
    }

    it("should be able to convert to and from dot syntax") {

      val jsonPath = ".animals[*].dogs[2].collie.rover['@name']"

      val expected = IrNodePathEmpty <~ "animals" <~ "*" <~ "dogs" <~ 2 <~ "collie" <~ "rover" <@ "name"

      PactPath.fromPactPath(jsonPath) match {
        case PactPathParseSuccess(irNodePath) =>
          withClue("Created paths are equal") {
            irNodePath === expected shouldEqual true
          }

          withClue("Rendered path are as expected") {
            irNodePath.renderAsString shouldEqual jsonPath
          }

        case e: PactPathParseFailure =>
          fail(e.errorString)
      }

    }

    it("should be able to convert to and from bracket syntax") {

      val jsonPathA = "['animals'][*]['dogs'][2]['collie']['rover']['@name']"
      val jsonPathB = """["animals"][*]["dogs"][2]["collie"]["rover"]["@name"]"""

      val expected = IrNodePathEmpty <~ "animals" <~ "*" <~ "dogs" <~ 2 <~ "collie" <~ "rover" <@ "name"

      withClue("Single and double quotes are equivalent") {
        PactPath.fromPactPath(jsonPathA) === PactPath.fromPactPath(jsonPathB) shouldEqual true
      }

      PactPath.fromPactPath(jsonPathA) match {
        case PactPathParseSuccess(irNodePath) =>

          withClue("Created paths are equal") {
            irNodePath === expected shouldEqual true
          }

          val expectedRender = ".animals[*].dogs[2].collie.rover['@name']" // renders as dot syntax

          withClue("Rendered path are as expected") {
            irNodePath.renderAsString shouldEqual expectedRender
          }

        case e: PactPathParseFailure =>
          fail(e.errorString)
      }

    }

    it("should be able to convert to and from a combination of dot and bracket syntax") {

      val jsonPath = ".animals[*].dogs[2]['collie'].rover['#text']"

      val expected = IrNodePathEmpty <~ "animals" <~ "*" <~ "dogs" <~ 2 <~ "collie" <~ "rover" text

      PactPath.fromPactPath(jsonPath) match {
        case PactPathParseSuccess(irNodePath) =>

          withClue("Created paths are equal") {
            irNodePath === expected shouldEqual true
          }

          val expectedRender = ".animals[*].dogs[2].collie.rover['#text']" // renders as dot syntax

          withClue("Rendered path are as expected") {
            irNodePath.renderAsString shouldEqual expectedRender
          }

        case e: PactPathParseFailure =>
          fail(e.errorString)
      }

    }

    it("should be able to cope with * fields") {
      val jsonPath = "$.body.foo.*"

      val expected = IrNodePathEmpty <~ "foo" <*

      PactPath.fromPactPath(jsonPath) match {
        case PactPathParseSuccess(irNodePath) =>
          irNodePath === expected shouldEqual true

        case e: PactPathParseFailure =>
          fail(e.errorString)
      }
    }

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

    it("should be able to check paths with attributes") {
      val expected =
        IrNodePathFieldAttribute(
          "salt",
          IrNodePathField(
            "chips",
            IrNodePathField(
              "fish",
              IrNodePathEmpty
            )
          )
        )

      (IrNodePathEmpty <~ "fish" <~ "chips" <@ "salt") === expected shouldEqual true
    }

    it("should be able to check paths with text elements") {
      val expected =
        IrNodePathTextElement(
          IrNodePathField(
            "chips",
            IrNodePathField(
              "fish",
              IrNodePathEmpty
            )
          )
        )

      (IrNodePathEmpty <~ "fish" <~ "chips" text) === expected shouldEqual true
    }

    it("should check equality with wildcards") {

      (IrNodePath.fromPactPath("$.body.animals[*].children[*].*").toEither, IrNodePath.fromPactPath("$.body.animals[0].children[0].age").toEither) match {
        case (Right(rulePath), Right(nodePath)) =>
          rulePath === nodePath shouldEqual true
        case (Left(e), _) =>
          fail(e)
        case (_, Left(e)) =>
          fail(e)
      }

    }

  }

}

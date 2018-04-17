package com.itv.scalapact.plugin.shared

import org.scalatest.{FunSpec, Matchers}

class ScalaPactVerifyCommandSpec extends FunSpec with Matchers {

  describe("Mergeing the providerStates and providerStateMatcher") {

    it("should be able to combine them into a single function") {

      // We perform a side effect just to prove the function is being called.
      var result = ""

      val directPactStates: Seq[(String, String => Boolean)] = Seq(
        (
          "abc",
          (_: String) => {
            result = "abc"
            true
          }
        )
      )

      val patternMatchedStates: PartialFunction[String, Boolean] = {
        case "def" =>
          result = "def"
          true
      }

      val combined =
        ScalaPactVerifyCommand.combineProviderStatesIntoTotalFunction(directPactStates, patternMatchedStates)

      withClue("With key: abc") {
        result = ""
        combined("abc") shouldEqual true
        result shouldEqual "abc"
      }

      withClue("With key: def") {
        result = ""
        combined("def") shouldEqual true
        result shouldEqual "def"
      }

      withClue("With key: fish") {
        result = ""
        combined("fish") shouldEqual false
        result shouldEqual ""
      }

    }

    it("should be able to combine direct pact states and default providerStateMatcher") {

      // We perform a side effect just to prove the function is being called.
      var result = ""

      val directPactStates: Seq[(String, String => Boolean)] = Seq(
        (
          "abc",
          (_: String) => {
            result = "abc"
            true
          }
        )
      )

      val patternMatchedStates: PartialFunction[String, Boolean] = { case (_: String) => false }

      val combined =
        ScalaPactVerifyCommand.combineProviderStatesIntoTotalFunction(directPactStates, patternMatchedStates)

      withClue("With key: abc") {
        result = ""
        combined("abc") shouldEqual true
        result shouldEqual "abc"
      }

      withClue("With key: fish") {
        result = ""
        combined("fish") shouldEqual false
        result shouldEqual ""
      }

    }

    it("should be able to combine default direct pact states and providerStateMatcher") {

      // We perform a side effect just to prove the function is being called.
      var result = ""

      val directPactStates: Seq[(String, String => Boolean)] = Seq()

      val patternMatchedStates: PartialFunction[String, Boolean] = {
        case "def" =>
          result = "def"
          true
      }

      val combined =
        ScalaPactVerifyCommand.combineProviderStatesIntoTotalFunction(directPactStates, patternMatchedStates)

      withClue("With key: def") {
        result = ""
        combined("def") shouldEqual true
        result shouldEqual "def"
      }

      withClue("With key: fish") {
        result = ""
        combined("fish") shouldEqual false
        result shouldEqual ""
      }

    }

    it("should be able to combine default direct pact states and default providerStateMatcher") {

      // We perform a side effect just to prove the function is being called.
      var result = ""

      val directPactStates: Seq[(String, String => Boolean)]     = Seq()
      val patternMatchedStates: PartialFunction[String, Boolean] = { case (_: String) => false }

      val combined =
        ScalaPactVerifyCommand.combineProviderStatesIntoTotalFunction(directPactStates, patternMatchedStates)

      withClue("With key: fish") {
        result = ""
        combined("fish") shouldEqual false
        result shouldEqual ""
      }

    }
  }

}

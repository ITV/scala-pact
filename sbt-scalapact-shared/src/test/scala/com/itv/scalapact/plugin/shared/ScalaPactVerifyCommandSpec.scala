package com.itv.scalapact.plugin.shared

import com.itv.scalapact.shared.ProviderStateResult
import com.itv.scalapact.shared.ProviderStateResult.SetupProviderState
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers

class ScalaPactVerifyCommandSpec extends AnyFunSpec with Matchers {

  describe("Mergeing the providerStates and providerStateMatcher") {

    it("should be able to combine them into a single function") {

      // We perform a side effect just to prove the function is being called.
      var result = ""

      val directPactStates: Seq[(String, SetupProviderState)] = Seq(
        (
          "abc",
          (_: String) => {
            result = "abc"
            ProviderStateResult(true)
          }
        )
      )

      val patternMatchedStates: PartialFunction[String, ProviderStateResult] = { case "def" =>
        result = "def"
        ProviderStateResult(true)
      }

      val combined =
        ScalaPactVerifyCommand.combineProviderStatesIntoTotalFunction(directPactStates, patternMatchedStates)

      withClue("With key: abc") {
        result = ""
        combined("abc").result shouldEqual true
        result shouldEqual "abc"
      }

      withClue("With key: def") {
        result = ""
        combined("def").result shouldEqual true
        result shouldEqual "def"
      }

      withClue("With key: fish") {
        result = ""
        combined("fish").result shouldEqual false
        result shouldEqual ""
      }

    }

    it("should be able to combine direct pact states and default providerStateMatcher") {

      // We perform a side effect just to prove the function is being called.
      var result = ""

      val directPactStates: Seq[(String, SetupProviderState)] = Seq(
        (
          "abc",
          (_: String) => {
            result = "abc"
            ProviderStateResult(true)
          }
        )
      )

      val patternMatchedStates: PartialFunction[String, ProviderStateResult] = { case _: String =>
        ProviderStateResult()
      }

      val combined =
        ScalaPactVerifyCommand.combineProviderStatesIntoTotalFunction(directPactStates, patternMatchedStates)

      withClue("With key: abc") {
        result = ""
        combined("abc").result shouldEqual true
        result shouldEqual "abc"
      }

      withClue("With key: fish") {
        result = ""
        combined("fish").result shouldEqual false
        result shouldEqual ""
      }

    }

    it("should be able to combine default direct pact states and providerStateMatcher") {

      // We perform a side effect just to prove the function is being called.
      var result = ""

      val directPactStates: Seq[(String, SetupProviderState)] = Seq()

      val patternMatchedStates: PartialFunction[String, ProviderStateResult] = { case "def" =>
        result = "def"
        ProviderStateResult(true)
      }

      val combined =
        ScalaPactVerifyCommand.combineProviderStatesIntoTotalFunction(directPactStates, patternMatchedStates)

      withClue("With key: def") {
        result = ""
        combined("def").result shouldEqual true
        result shouldEqual "def"
      }

      withClue("With key: fish") {
        result = ""
        combined("fish").result shouldEqual false
        result shouldEqual ""
      }

    }

    it("should be able to combine default direct pact states and default providerStateMatcher") {

      // We perform a side effect just to prove the function is being called.
      var result = ""

      val directPactStates: Seq[(String, SetupProviderState)] = Seq()
      val patternMatchedStates: PartialFunction[String, ProviderStateResult] = { case _: String =>
        ProviderStateResult()
      }

      val combined =
        ScalaPactVerifyCommand.combineProviderStatesIntoTotalFunction(directPactStates, patternMatchedStates)

      withClue("With key: fish") {
        result = ""
        combined("fish").result shouldEqual false
        result shouldEqual ""
      }

    }
  }

}

package com.itv.scalapact.plugin.shared

import com.itv.scalapact.shared.InteractionRequest
import com.itv.scalapactcore.verifier.Verifier.SetupProviderState
import org.scalatest.{FunSpec, Matchers}

class ScalaPactVerifyCommandSpec extends FunSpec with Matchers {

  describe("Mergeing the providerStates and providerStateMatcher") {

    it("should be able to combine them into a single function") {

      // We perform a side effect just to prove the function is being called.
      var result = ""

      val directPactStates: Seq[(String, SetupProviderState)] = Seq(
        (
          "abc",
          (_: String) => {
            result = "abc"
            (true, identity[InteractionRequest])
          }
        )
      )

      val patternMatchedStates: PartialFunction[String, (Boolean, InteractionRequest => InteractionRequest)] = {
        case "def" =>
          result = "def"
          (true, identity[InteractionRequest])
      }

      val combined =
        ScalaPactVerifyCommand.combineProviderStatesIntoTotalFunction(directPactStates, patternMatchedStates)

      withClue("With key: abc") {
        result = ""
        combined("abc")._1 shouldEqual true
        result shouldEqual "abc"
      }

      withClue("With key: def") {
        result = ""
        combined("def")._1 shouldEqual true
        result shouldEqual "def"
      }

      withClue("With key: fish") {
        result = ""
        combined("fish")._1 shouldEqual false
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
            (true, identity[InteractionRequest])
          }
        )
      )

      val patternMatchedStates: PartialFunction[String, (Boolean, InteractionRequest => InteractionRequest)] = { case (_: String) => (false, identity[InteractionRequest]) }

      val combined =
        ScalaPactVerifyCommand.combineProviderStatesIntoTotalFunction(directPactStates, patternMatchedStates)

      withClue("With key: abc") {
        result = ""
        combined("abc")._1 shouldEqual true
        result shouldEqual "abc"
      }

      withClue("With key: fish") {
        result = ""
        combined("fish")._1 shouldEqual false
        result shouldEqual ""
      }

    }

    it("should be able to combine default direct pact states and providerStateMatcher") {

      // We perform a side effect just to prove the function is being called.
      var result = ""

      val directPactStates: Seq[(String, SetupProviderState)] = Seq()

      val patternMatchedStates: PartialFunction[String, (Boolean, InteractionRequest => InteractionRequest)] = {
        case "def" =>
          result = "def"
          (true, identity[InteractionRequest])
      }

      val combined =
        ScalaPactVerifyCommand.combineProviderStatesIntoTotalFunction(directPactStates, patternMatchedStates)

      withClue("With key: def") {
        result = ""
        combined("def")._1 shouldEqual true
        result shouldEqual "def"
      }

      withClue("With key: fish") {
        result = ""
        combined("fish")._1 shouldEqual false
        result shouldEqual ""
      }

    }

    it("should be able to combine default direct pact states and default providerStateMatcher") {

      // We perform a side effect just to prove the function is being called.
      var result = ""

      val directPactStates: Seq[(String, SetupProviderState)]     = Seq()
      val patternMatchedStates: PartialFunction[String, (Boolean, InteractionRequest => InteractionRequest)] = { case (_: String) => (false, identity[InteractionRequest]) }

      val combined =
        ScalaPactVerifyCommand.combineProviderStatesIntoTotalFunction(directPactStates, patternMatchedStates)

      withClue("With key: fish") {
        result = ""
        combined("fish")._1 shouldEqual false
        result shouldEqual ""
      }

    }
  }

}

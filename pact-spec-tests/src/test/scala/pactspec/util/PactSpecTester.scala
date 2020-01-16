package pactspec.util

import com.itv.scalapactcore.common.matching.InteractionMatchers._
import com.itv.scalapactcore.common.matching.{MatchOutcomeFailed, MatchOutcomeSuccess}
import com.itv.scalapact.shared.{Interaction, InteractionRequest, InteractionResponse}
import org.scalatest.{FunSpec, Matchers}

trait PactSpecTester extends FunSpec with Matchers {

  import com.itv.scalapact.json._

  val pactSpecVersion: String

  val fileNameFromPath: String => String = path => path.split("/").reverse.headOption.getOrElse(path)

  protected val fetchRequestSpec: String => StrictTestMode => (RequestSpec, StrictTestMode, String) = path =>
    testMode =>
      (PactSpecLoader
         .deserializeRequestSpec(PactSpecLoader.fromResource(pactSpecVersion, path))
         .getOrElse(throw new Exception("Failed to deserialise request spec")),
       testMode,
       fileNameFromPath(path))

  protected def testRequestSpecs(specFiles: List[(RequestSpec, StrictTestMode, String)]): Unit =
    specFiles.foreach { specAndMode =>
      val spec = specAndMode._1
      val mode = specAndMode._2
      val path = specAndMode._3
      val i    = Interaction(None, None, "", spec.expected, InteractionResponse(None, None, None, None))

      mode match {
        case StrictOnly =>
          doRequestMatch(spec, i, strictMatching = false, shouldMatch = !spec.`match`, path)
          doRequestMatch(spec, i, strictMatching = true, shouldMatch = spec.`match`, path)

        case StrictAndNonStrict =>
          doRequestMatch(spec, i, strictMatching = false, shouldMatch = spec.`match`, path)
          doRequestMatch(spec, i, strictMatching = true, shouldMatch = spec.`match`, path)
      }

    }

  private def doRequestMatch(spec: RequestSpec,
                             i: Interaction,
                             strictMatching: Boolean,
                             shouldMatch: Boolean,
                             path: String): Unit =
    matchSingleRequest(strictMatching, i.request.matchingRules, i.request, spec.actual) match {
      case MatchOutcomeSuccess =>

        if (!shouldMatch)
          fail(
            makeErrorString(shouldMatch,
                            path,
                            spec.comment,
                            strictMatching,
                            spec.actual.renderAsString,
                            spec.expected.renderAsString,
                            "")
          )

      case e: MatchOutcomeFailed =>
        if (shouldMatch)
          fail(
            makeErrorString(shouldMatch,
                            path,
                            spec.comment,
                            strictMatching,
                            spec.actual.renderAsString,
                            spec.expected.renderAsString,
                            e.renderDifferences)
          )
    }

  protected val fetchResponseSpec: String => StrictTestMode => (ResponseSpec, StrictTestMode, String) = path =>
    testMode =>
      (PactSpecLoader
         .deserializeResponseSpec(PactSpecLoader.fromResource(pactSpecVersion, path))
         .getOrElse(throw new Exception("Failed to deserialise response spec")),
       testMode,
       fileNameFromPath(path))

  protected def testResponseSpecs(specFiles: List[(ResponseSpec, StrictTestMode, String)]): Unit =
    specFiles.foreach { specAndMode =>
      val spec = specAndMode._1
      val mode = specAndMode._2
      val path = specAndMode._3
      val i    = Interaction(None, None, "", InteractionRequest(None, None, None, None, None, None), spec.expected)

      mode match {

        case StrictOnly =>
          doResponseMatch(spec, i, strictMatching = false, shouldMatch = !spec.`match`, path)
          doResponseMatch(spec, i, strictMatching = true, shouldMatch = spec.`match`, path)

        case StrictAndNonStrict =>
          doResponseMatch(spec, i, strictMatching = false, shouldMatch = spec.`match`, path)
          doResponseMatch(spec, i, strictMatching = true, shouldMatch = spec.`match`, path)
      }

    }

  private def doResponseMatch(spec: ResponseSpec,
                              i: Interaction,
                              strictMatching: Boolean,
                              shouldMatch: Boolean,
                              path: String): Unit =
    matchSingleResponse(strictMatching, i.response.matchingRules, i.response, spec.actual) match {
      case MatchOutcomeSuccess =>
        if (!shouldMatch)
          fail(
            makeErrorString(shouldMatch,
                            path,
                            spec.comment,
                            strictMatching,
                            spec.actual.renderAsString,
                            spec.expected.renderAsString,
                            "")
          )

      case e: MatchOutcomeFailed =>
        if (shouldMatch)
          fail(
            makeErrorString(shouldMatch,
                            path,
                            spec.comment,
                            strictMatching,
                            spec.actual.renderAsString,
                            spec.expected.renderAsString,
                            e.renderDifferences)
          )
    }

  private def makeErrorString(shouldMatch: Boolean,
                              path: String,
                              comment: String,
                              strictMatching: Boolean,
                              actual: String,
                              expected: String,
                              differences: String): String =
    s"Expected match: $shouldMatch\n[$path] " + comment + "\nStrict matching: '" + strictMatching + "'\n\nExpected:\n" + expected + "\nActual:\n" + actual + "\nMatch Errors: [\n" + differences + "\n]"

}

sealed trait StrictTestMode

// Note to self: This should not be a thing.
// Strict means 'Pact standard' and non-strict is a relaxing of the rules.
// There should be no permissive only tests
//case object NonStrictOnly extends StrictTestMode

case object StrictOnly         extends StrictTestMode
case object StrictAndNonStrict extends StrictTestMode

package com.itv.scalapactcore.pactspec.util

import com.itv.scalapactcore.{Interaction, InteractionRequest, InteractionResponse}
import com.itv.scalapactcore.common.matching.InteractionMatchers._
import org.scalatest.{FunSpec, Matchers}

import scalaz.{-\/, \/-}

trait PactSpecTester extends FunSpec with Matchers {

  val pactSpecVersion: String

  val fileNameFromPath: String => String = path =>
    path.split("/").reverse.headOption.getOrElse(path)

  protected val fetchRequestSpec: String => StrictTestMode => (RequestSpec, StrictTestMode, String) = path => testMode =>
    (PactSpecLoader.deserializeRequestSpec(PactSpecLoader.fromResource(pactSpecVersion, path)).getOrElse(throw new Exception("Failed to deserialise request spec")), testMode, fileNameFromPath(path))

  protected def testRequestSpecs(specFiles: List[(RequestSpec, StrictTestMode, String)]): Unit = {
    specFiles.foreach { specAndMode =>

      val spec = specAndMode._1
      val mode = specAndMode._2
      val path = specAndMode._3
      val i = Interaction(None, "", spec.expected, InteractionResponse(None, None, None, None))

      mode match {
        case NonStrictOnly =>
          doRequestMatch(spec, i, strictMatching = false, shouldMatch = spec.`match`, path)
          doRequestMatch(spec, i, strictMatching = true, shouldMatch = !spec.`match`, path)

        case StrictOnly =>
          doRequestMatch(spec, i, strictMatching = false, shouldMatch = !spec.`match`, path)
          doRequestMatch(spec, i, strictMatching = true, shouldMatch = spec.`match`, path)

        case StrictAndNonStrict =>
          doRequestMatch(spec, i, strictMatching = false, shouldMatch = spec.`match`, path)
          doRequestMatch(spec, i, strictMatching = true, shouldMatch = spec.`match`, path)
      }

    }
  }

  private def doRequestMatch(spec: RequestSpec, i: Interaction, strictMatching: Boolean, shouldMatch: Boolean, path: String): Unit = {
    matchRequest(strictMatching)(i :: Nil)(spec.actual) match {
      case Right(_) =>
        // Found a match
        if (shouldMatch) 1 shouldEqual 1 // It's here, so the test should pass. Can't find a 'pass' method...
        else fail(makeErrorString(path, spec.comment, strictMatching, spec.actual.toString, spec.expected.toString))

      case Left(_) =>
        // Failed to match
        if (shouldMatch) fail(makeErrorString(path, spec.comment, strictMatching, spec.actual.toString, spec.expected.toString))
        else 1 shouldEqual 1 // It's here, so the test should pass. Can't find a 'pass' method...
    }
  }



  protected val fetchResponseSpec: String => StrictTestMode => (ResponseSpec, StrictTestMode, String) = path => testMode =>
    (PactSpecLoader.deserializeResponseSpec(PactSpecLoader.fromResource(pactSpecVersion, path)).getOrElse(throw new Exception("Failed to deserialise response spec")), testMode, fileNameFromPath(path))

  protected def testResponseSpecs(specFiles: List[(ResponseSpec, StrictTestMode, String)]): Unit = {
    specFiles.foreach { specAndMode =>

      val spec = specAndMode._1
      val mode = specAndMode._2
      val path = specAndMode._3
      val i = Interaction(None, "", InteractionRequest(None, None, None, None, None, None), spec.expected)

      mode match {
        case NonStrictOnly =>
          doResponseMatch(spec, i, strictMatching = false, shouldMatch = spec.`match`, path)
          doResponseMatch(spec, i, strictMatching = true, shouldMatch = !spec.`match`, path)

        case StrictOnly =>
          doResponseMatch(spec, i, strictMatching = false, shouldMatch = !spec.`match`, path)
          doResponseMatch(spec, i, strictMatching = true, shouldMatch = spec.`match`, path)

        case StrictAndNonStrict =>
          doResponseMatch(spec, i, strictMatching = false, shouldMatch = spec.`match`, path)
          doResponseMatch(spec, i, strictMatching = true, shouldMatch = spec.`match`, path)
      }

    }
  }

  private def doResponseMatch(spec: ResponseSpec, i: Interaction, strictMatching: Boolean, shouldMatch: Boolean, path: String): Unit = {
    matchResponse(strictMatching)(i :: Nil)(spec.actual) match {
      case Right(_) =>
        // Found a match
        if (shouldMatch) 1 shouldEqual 1 // It's here, so the test should pass. Can't find a 'pass' method...
        else fail(makeErrorString(path, spec.comment, strictMatching, spec.actual.toString, spec.expected.toString))

      case Left(_) =>
        // Failed to match
        if (shouldMatch) fail(makeErrorString(path, spec.comment, strictMatching, spec.actual.toString, spec.expected.toString))
        else 1 shouldEqual 1 // It's here, so the test should pass. Can't find a 'pass' method...
    }
  }

  private def makeErrorString(path: String, comment: String, strictMatching: Boolean, actual: String, expected: String): String = {
    s"[$path] " + comment + "\nStrict matching: '" + strictMatching + "'\nActual: " + actual + "\nExpected: " + expected
  }

}

sealed trait StrictTestMode

case object NonStrictOnly extends StrictTestMode
case object StrictOnly extends StrictTestMode
case object StrictAndNonStrict extends StrictTestMode

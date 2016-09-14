package com.itv.scalapactcore.pactspec.util

import com.itv.scalapactcore.{Interaction, InteractionRequest, InteractionResponse}
import com.itv.scalapactcore.common.InteractionMatchers._
import org.scalatest.{FunSpec, Matchers}

import scalaz.{-\/, \/-}

trait PactSpecTester extends FunSpec with Matchers {

  val pactSpecVersion: String

  protected val fetchRequestSpec: String => StrictTestMode => (RequestSpec, StrictTestMode) = path => testMode =>
    (PactSpecLoader.deserializeRequestSpec(PactSpecLoader.fromResource(pactSpecVersion, path)).get, testMode)

  protected def testRequestSpecs(specFiles: List[(RequestSpec, StrictTestMode)]): Unit = {
    specFiles.foreach { specAndMode =>

      val spec = specAndMode._1
      val mode = specAndMode._2
      val i = Interaction(None, "", spec.expected, InteractionResponse(None, None, None, None))

      mode match {
        case NonStrictOnly =>
          doRequestMatch(spec, i, strictMatching = false)

        case StrictOnly =>
          doRequestMatch(spec, i, strictMatching = true)

        case StrictAndNonStrict =>
          doRequestMatch(spec, i, strictMatching = false)
          doRequestMatch(spec, i, strictMatching = true)
      }

    }
  }

  private def doRequestMatch(spec: RequestSpec, i: Interaction, strictMatching: Boolean): Unit = {
    matchRequest(strictMatching)(i :: Nil)(spec.actual) match {
      case \/-(r) =>
        if (spec.`match`) 1 shouldEqual 1 // It's here, so the test should pass. Can't find a 'pass' method...
        else fail(spec.comment + ", with strict matching '" + strictMatching + "',  actual: " + spec.actual + ",  expected: " + spec.expected)

      case -\/(l) =>
        if (spec.`match`) fail(spec.comment + ", with strict matching '" + strictMatching + "',  actual: " + spec.actual + ",  expected: " + spec.expected)
        else 1 shouldEqual 1 // It's here, so the test should pass. Can't find a 'pass' method...
    }
  }



  protected val fetchResponseSpec: String => StrictTestMode => (ResponseSpec, StrictTestMode) = path => testMode =>
    (PactSpecLoader.deserializeResponseSpec(PactSpecLoader.fromResource(pactSpecVersion, path)).get, testMode)

  protected def testResponseSpecs(specFiles: List[(ResponseSpec, StrictTestMode)]): Unit = {
    specFiles.foreach { specAndMode =>

      val spec = specAndMode._1
      val mode = specAndMode._2
      val i = Interaction(None, "", InteractionRequest(None, None, None, None, None, None), spec.expected)

      mode match {
        case NonStrictOnly =>
          doResponseMatch(spec, i, strictMatching = false, shouldMatch = spec.`match`)
          doResponseMatch(spec, i, strictMatching = true, shouldMatch = !spec.`match`)

        case StrictOnly =>
          doResponseMatch(spec, i, strictMatching = false, shouldMatch = !spec.`match`)
          doResponseMatch(spec, i, strictMatching = true, shouldMatch = spec.`match`)

        case StrictAndNonStrict =>
          doResponseMatch(spec, i, strictMatching = false, shouldMatch = spec.`match`)
          doResponseMatch(spec, i, strictMatching = true, shouldMatch = spec.`match`)
      }

    }
  }

  private def doResponseMatch(spec: ResponseSpec, i: Interaction, strictMatching: Boolean, shouldMatch: Boolean): Unit = {
    matchResponse(strictMatching)(i :: Nil)(spec.actual) match {
      case \/-(_) =>
        // Found a match
        if (shouldMatch) 1 shouldEqual 1 // It's here, so the test should pass. Can't find a 'pass' method...
        else fail(spec.comment + ", with strict matching '" + strictMatching + "',  actual: " + spec.actual + ",  expected: " + spec.expected)

      case -\/(_) =>
        // Failed to match
        if (shouldMatch) fail(spec.comment + ", with strict matching '" + strictMatching + "',  actual: " + spec.actual + ",  expected: " + spec.expected)
        else 1 shouldEqual 1 // It's here, so the test should pass. Can't find a 'pass' method...
    }
  }

}

sealed trait StrictTestMode

case object NonStrictOnly extends StrictTestMode
case object StrictOnly extends StrictTestMode
case object StrictAndNonStrict extends StrictTestMode

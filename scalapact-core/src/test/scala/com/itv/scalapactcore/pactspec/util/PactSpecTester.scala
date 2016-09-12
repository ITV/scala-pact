package com.itv.scalapactcore.pactspec.util

import com.itv.scalapactcore.{Interaction, InteractionRequest, InteractionResponse}
import com.itv.scalapactcore.common.InteractionMatchers._
import org.scalatest.{FunSpec, Matchers}

import scalaz.{-\/, \/-}

trait PactSpecTester extends FunSpec with Matchers {

  val pactSpecVersion: String

  protected val fetchRequestSpec: String => RequestSpec = path =>
    PactSpecLoader.deserializeRequestSpec(PactSpecLoader.fromResource(pactSpecVersion, path)).get

  protected def testRequestSpecs(specFiles: List[RequestSpec]): Unit = {
    specFiles.foreach { spec =>

      val i = Interaction(None, "", spec.expected, InteractionResponse(None, None, None, None))

      matchRequest(i :: Nil)(spec.actual) match {
        case \/-(r) =>
          if (spec.`match`) 1 shouldEqual 1 // It's here, so the test should pass. Can't find a 'pass' method...
          else fail(spec.comment + ",  actual: " + spec.actual + ",  expected: " + spec.expected)

        case -\/(l) =>
          if (spec.`match`) fail(spec.comment + ",  actual: " + spec.actual + ",  expected: " + spec.expected)
          else 1 shouldEqual 1 // It's here, so the test should pass. Can't find a 'pass' method...
      }

    }
  }

  protected val fetchResponseSpec: String => ResponseSpec = path =>
    PactSpecLoader.deserializeResponseSpec(PactSpecLoader.fromResource(pactSpecVersion, path)).get

  protected def testResponseSpecs(specFiles: List[ResponseSpec]): Unit = {
    specFiles.foreach { spec =>

      val i = Interaction(None, "", InteractionRequest(None, None, None, None, None, None), spec.expected)

      matchResponse(i :: Nil)(spec.actual) match {
        case \/-(r) =>
          if (spec.`match`) 1 shouldEqual 1 // It's here, so the test should pass. Can't find a 'pass' method...
          else fail(spec.comment + ",  actual: " + spec.actual + ",  expected: " + spec.expected)

        case -\/(l) =>
          if (spec.`match`) fail(spec.comment + ",  actual: " + spec.actual + ",  expected: " + spec.expected)
          else 1 shouldEqual 1 // It's here, so the test should pass. Can't find a 'pass' method...
      }

    }
  }

}

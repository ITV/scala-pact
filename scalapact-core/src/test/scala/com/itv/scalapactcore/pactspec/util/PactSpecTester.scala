package com.itv.scalapactcore.pactspec.util

import com.itv.scalapactcore.{Interaction, InteractionResponse}
import com.itv.scalapactcore.common.InteractionMatchers._
import org.scalatest.{FunSpec, Matchers}

import scalaz.{-\/, \/-}

trait PactSpecTester extends FunSpec with Matchers {

  val pactSpecVersion: String

  protected val fetchSpec: String => RequestSpec = path =>
    PactSpecLoader.deserializeRequestSpec(PactSpecLoader.fromResource(pactSpecVersion, path)).get

  protected def testSpecs(specFiles: List[RequestSpec]): Unit = {
    specFiles.foreach { spec =>

      val i = Interaction(None, "", spec.expected, InteractionResponse(None, None, None, None))

      matchRequest(i :: Nil)(spec.actual) match {
        case \/-(r) =>
          if (spec.`match`) 1 shouldEqual 1 // It's here, so the test should pass. Can't find a 'pass' method...
          else fail(spec.comment)

        case -\/(l) =>
          if (spec.`match`) fail(spec.comment)
          else 1 shouldEqual 1 // It's here, so the test should pass. Can't find a 'pass' method...
      }

    }
  }

}

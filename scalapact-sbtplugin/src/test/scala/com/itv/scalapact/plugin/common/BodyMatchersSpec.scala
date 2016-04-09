package com.itv.scalapact.plugin.common

import org.scalatest.{FunSpec, Matchers}

import scala.language.implicitConversions

class BodyMatchersSpec extends FunSpec with Matchers {

  implicit def toOption[A](thing: A): Option[A] = Option(thing)

  import InteractionMatchers._

  describe("Matching bodies") {

    it("should do trivial equality testing") {

      matchBodies(Map.empty[String, String])("hello")("hello") shouldEqual true
      matchBodies(Map.empty[String, String])("hello")("this") shouldEqual false

    }

    it("should handle no body and additional body matching") {

      withClue("None, None") {
        matchBodies(Map.empty[String, String])(None)(None) shouldEqual true
      }

      withClue("None, Some") {
        matchBodies(Map.empty[String, String])(None)("this") shouldEqual true
      }

      withClue("Some, None") {
        matchBodies(Map.empty[String, String])("this")(None) shouldEqual false
      }

    }

  }

}

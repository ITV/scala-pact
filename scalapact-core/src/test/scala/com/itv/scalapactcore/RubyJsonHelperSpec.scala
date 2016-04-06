package com.itv.scalapactcore

import org.scalatest.{FunSpec, Matchers}

class RubyJsonHelperSpec extends FunSpec with Matchers {

  describe("Handling ruby json") {

    it("should be able to extract the provider") {

      RubyJsonHelper.extractPactActor("provider")(PactFileExamples.simpleAsRubyString) shouldEqual Some(PactActor("provider"))

    }

    it("should be able to extract the consumer") {

      RubyJsonHelper.extractPactActor("consumer")(PactFileExamples.simpleAsRubyString) shouldEqual Some(PactActor("consumer"))

    }

    it("should be able to extract a list of interactions paired with their bodies") {

      RubyJsonHelper.extractInteractions(PactFileExamples.simpleAsRubyString) shouldEqual Some(Nil)

    }

  }

}

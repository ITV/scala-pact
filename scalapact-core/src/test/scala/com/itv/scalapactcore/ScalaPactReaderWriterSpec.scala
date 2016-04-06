package com.itv.scalapactcore

import org.scalatest.{FunSpec, Matchers}

class ScalaPactReaderWriterSpec extends FunSpec with Matchers {

  describe("Reading and writing a homogeneous Pact files") {

    it("should be able to read Pact files") {
      val pactEither = ScalaPactReader.jsonStringToPact(PactFileExamples.simpleAsRubyString)

      pactEither.toOption.get shouldEqual PactFileExamples.simpleRuby
    }

    it("should be able to write Pact files") {

      val written = ScalaPactWriter.pactToJsonString(PactFileExamples.simpleRuby)

      val expected = PactFileExamples.simpleAsRubyString

      written shouldEqual expected

    }

    it("should be able to eat it's own dog food") {

      val json = ScalaPactWriter.pactToJsonString(PactFileExamples.simpleRuby)

      val pact = ScalaPactReader.jsonStringToPact(json).toOption.get

      val `reJson'd` = ScalaPactWriter.pactToJsonString(pact)

      `reJson'd` shouldEqual PactFileExamples.simpleAsRubyString
      pact shouldEqual PactFileExamples.simpleRuby

    }

    it("should be able to read ruby format json") {
      val pactEither = ScalaPactReader.rubyJsonToPact(PactFileExamples.simpleAsRubyString)

      pactEither.toOption.get shouldEqual PactFileExamples.simpleRuby
    }

    it("should be able to write a pact file in ruby format") {

      val written = ScalaPactWriter.pactToRubyJsonString(PactFileExamples.simpleRuby)

      val expected = PactFileExamples.simpleAsRubyString

      written shouldEqual expected

    }

    it("should be able to eat it's own dog food with no body") {

      val json = ScalaPactWriter.pactToJsonString(PactFileExamples.verySimple)

      val pact = ScalaPactReader.jsonStringToPact(json).toOption.get

      val `reJson'd` = ScalaPactWriter.pactToJsonString(pact)

      `reJson'd` shouldEqual PactFileExamples.verySimpleAsString
      pact shouldEqual PactFileExamples.verySimple

    }

    it("should be able to read ruby format json with no body") {
      val pactEither = ScalaPactReader.rubyJsonToPact(PactFileExamples.verySimpleAsString)

      pactEither.toOption.get shouldEqual PactFileExamples.verySimple
    }

    it("should be able to write a pact file in ruby format with no body") {

      val written = ScalaPactWriter.pactToRubyJsonString(PactFileExamples.verySimple)

      val expected = PactFileExamples.verySimpleAsString

      written shouldEqual expected
    }

  }

}


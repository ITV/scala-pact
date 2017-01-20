package com.itv.scalapactcore

import org.scalatest.{FunSpec, Matchers}

import argonaut.JsonParser._

class ScalaPactReaderWriterSpec extends FunSpec with Matchers {

  describe("Reading and writing a homogeneous Pact files") {

    it("should be able to read Pact files") {
      val pactEither = ScalaPactReader.jsonStringToPact(PactFileExamples.simpleAsString)

      pactEither.right.get shouldEqual PactFileExamples.simple
    }

    it("should be able to read Pact files using the old provider state key") {
      val pactEither = ScalaPactReader.jsonStringToPact(PactFileExamples.simpleOldProviderStateAsString)

      pactEither.right.get shouldEqual PactFileExamples.simple
    }

    it("should be able to write Pact files") {

      val written = ScalaPactWriter.pactToJsonString(PactFileExamples.simple)

      val expected = PactFileExamples.simpleAsString

      parse(written).toOption.get shouldEqual parse(expected).toOption.get
    }

    it("should be able to eat it's own dog food") {

      val json = ScalaPactWriter.pactToJsonString(PactFileExamples.simple)

      val pact = ScalaPactReader.jsonStringToPact(json).right.get

      val `reJson'd` = parse(ScalaPactWriter.pactToJsonString(pact)).toOption.get

      `reJson'd` shouldEqual parse(PactFileExamples.simpleAsString).toOption.get

      pact shouldEqual PactFileExamples.simple

    }

    it("should be able to read ruby format json") {
      val pactEither = ScalaPactReader.jsonStringToPact(PactFileExamples.simpleAsString)

      pactEither.right.get shouldEqual PactFileExamples.simple
    }

    it("should be able to write a pact file in ruby format") {

      val written = ScalaPactWriter.pactToJsonString(PactFileExamples.simple)

      val expected = PactFileExamples.simpleAsString

      written shouldEqual expected

    }

    it("should be able to eat it's own dog food with no body") {

      val json = ScalaPactWriter.pactToJsonString(PactFileExamples.verySimple)

      val pact = ScalaPactReader.jsonStringToPact(json).right.get

      val `reJson'd` = parse(ScalaPactWriter.pactToJsonString(pact)).toOption.get

      `reJson'd` shouldEqual parse(PactFileExamples.verySimpleAsString).toOption.get

      pact shouldEqual PactFileExamples.verySimple

    }

    it("should be able to read ruby format json with no body") {
      val pactEither = ScalaPactReader.jsonStringToPact(PactFileExamples.verySimpleAsString)

      pactEither.right.get shouldEqual PactFileExamples.verySimple
    }

    it("should be able to write a pact file in ruby format with no body") {

      val written = ScalaPactWriter.pactToJsonString(PactFileExamples.verySimple)

      val expected = PactFileExamples.verySimpleAsString

      parse(written).toOption.get shouldEqual parse(expected).toOption.get
    }

  }

}


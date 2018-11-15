package com.itv.scalapact.argonaut62

import argonaut.JsonParser.parse
import org.scalatest.{FunSpec, Matchers}

class ScalaPactReaderWriterSpec extends FunSpec with Matchers {

  val pactReader = new PactReader
  val pactWriter = new PactWriter

  describe("Reading and writing a homogeneous Pact files") {

    it("should be able to read Pact files") {
      val pactEither = pactReader.jsonStringToPact(PactFileExamples.simpleAsString)

      pactEither.right.get shouldEqual PactFileExamples.simple
    }

    it("should be able to read Pact files using the old provider state key") {
      val pactEither = pactReader.jsonStringToPact(PactFileExamples.simpleOldProviderStateAsString)

      pactEither.right.get shouldEqual PactFileExamples.simple
    }

    it("should be able to write Pact files") {

      val written = pactWriter.pactToJsonString(PactFileExamples.simple)

      val expected = PactFileExamples.simpleAsString

      parse(written).toOption.get shouldEqual parse(expected).toOption.get
    }

    it("should be able to eat it's own dog food") {

      val json = pactWriter.pactToJsonString(PactFileExamples.simple)

      val pact = pactReader.jsonStringToPact(json).right.get

      val `reJson'd` = parse(pactWriter.pactToJsonString(pact)).toOption.get

      `reJson'd` shouldEqual parse(PactFileExamples.simpleAsString).toOption.get

      pact shouldEqual PactFileExamples.simple

    }

    it("should be able to read ruby format json") {
      val pactEither = pactReader.jsonStringToPact(PactFileExamples.simpleAsString)

      pactEither.right.get shouldEqual PactFileExamples.simple
    }

    it("should be able to write a pact file in ruby format") {

      val written = pactWriter.pactToJsonString(PactFileExamples.simple)

      val expected = PactFileExamples.simpleAsString

      written shouldEqual expected

    }

    it("should be able to eat it's own dog food with no body") {

      val json = pactWriter.pactToJsonString(PactFileExamples.verySimple)

      val pact = pactReader.jsonStringToPact(json).right.get

      val `reJson'd` = parse(pactWriter.pactToJsonString(pact)).toOption.get

      `reJson'd` shouldEqual parse(PactFileExamples.verySimpleAsString).toOption.get

      pact shouldEqual PactFileExamples.verySimple

    }

    it("should be able to read ruby format json with no body") {
      val pactEither = pactReader.jsonStringToPact(PactFileExamples.verySimpleAsString)

      pactEither.right.get shouldEqual PactFileExamples.verySimple
    }

    it("should be able to write a pact file in ruby format with no body") {

      val written = pactWriter.pactToJsonString(PactFileExamples.verySimple)

      val expected = PactFileExamples.verySimpleAsString

      parse(written).toOption.get shouldEqual parse(expected).toOption.get
    }

    it("should be able to parse _links and metadata") {
      val pactEither = pactReader.jsonStringToPact(PactFileExamples.simpleWithLinksAndMetaDataAsString)

      pactEither.right.get shouldEqual PactFileExamples.simpleWithLinksAndMetaData
    }

    it("should be able to write Pact files and add metadata when missing") {

      val written = pactWriter.pactToJsonString(PactFileExamples.simple)

      val expected = PactFileExamples.simpleWithMetaDataAsString

      parse(written).toOption.get shouldEqual parse(expected).toOption.get
    }

  }

}

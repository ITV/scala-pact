package com.itv.scalapact.circe13

import io.circe.parser._
import org.scalatest.{FunSpec, OptionValues, Matchers}
import com.itv.scalapact.test.PactFileExamples

class ScalaPactReaderWriterSpec extends FunSpec with Matchers with OptionValues {

  val pactReader = new PactReader
  val pactWriter = new PactWriter

  val scalaPactVersion: String = "1.0.0"

  describe("Reading and writing a homogeneous Pact files") {

    it("should be able to read Pact files") {
      val pactEither = pactReader.jsonStringToScalaPact(PactFileExamples.simpleAsString)

      pactEither.toOption.value shouldEqual PactFileExamples.simple
    }

    it("should be able to read Pact files using the old provider state key") {
      val pactEither = pactReader.jsonStringToScalaPact(PactFileExamples.simpleOldProviderStateAsString)

      pactEither.toOption.value shouldEqual PactFileExamples.simple
    }

    it("should be able to write Pact files") {

      val written = pactWriter.pactToJsonString(PactFileExamples.simple, scalaPactVersion)

      val expected = PactFileExamples.simpleAsString

      parse(written).toOption.get shouldEqual parse(expected).toOption.get
    }

    it("should be able to eat it's own dog food") {

      val json = pactWriter.pactToJsonString(PactFileExamples.simple, scalaPactVersion)

      val pact = pactReader.jsonStringToScalaPact(json).toOption.value

      val `reJson'd` = parse(pactWriter.pactToJsonString(pact, scalaPactVersion)).toOption.get

      `reJson'd` shouldEqual parse(PactFileExamples.simpleAsString).toOption.get

      pact shouldEqual PactFileExamples.simple

    }

    it("should be able to read ruby format json") {
      val pactEither = pactReader.jsonStringToScalaPact(PactFileExamples.simpleAsString)

      pactEither.toOption.value shouldEqual PactFileExamples.simple
    }

    it("should be able to write a pact file in ruby format") {

      val written = pactWriter.pactToJsonString(PactFileExamples.simple, scalaPactVersion)

      val expected = PactFileExamples.simpleAsString

      written shouldEqual expected

    }

    it("should be able to eat it's own dog food with no body") {

      val json = pactWriter.pactToJsonString(PactFileExamples.verySimple, scalaPactVersion)

      val pact = pactReader.jsonStringToScalaPact(json).toOption.value

      val `reJson'd` = parse(pactWriter.pactToJsonString(pact, scalaPactVersion)).toOption.get

      `reJson'd` shouldEqual parse(PactFileExamples.verySimpleAsString).toOption.get

      pact shouldEqual PactFileExamples.verySimple

    }

    it("should be able to read ruby format json with no body") {
      val pactEither = pactReader.jsonStringToScalaPact(PactFileExamples.verySimpleAsString)

      pactEither.toOption.value shouldEqual PactFileExamples.verySimple
    }

    it("should be able to write a pact file in ruby format with no body") {

      val written = pactWriter.pactToJsonString(PactFileExamples.verySimple, scalaPactVersion)

      val expected = PactFileExamples.verySimpleAsString

      parse(written).toOption.get shouldEqual parse(expected).toOption.get
    }

    it("should be able to parse another example") {

      pactReader.jsonStringToScalaPact(PactFileExamples.anotherExample) match {
        case Left(e) =>
          fail(e)

        case Right(pact) =>
          pact.consumer.name shouldEqual "My Consumer"
          pact.provider.name shouldEqual "Their Provider Service"
          pact.interactions.head.response.body.get shouldEqual "Hello there!"
      }

    }

    it("should be able to parse _links and metadata") {
      val pactEither = pactReader.jsonStringToScalaPact(PactFileExamples.simpleWithLinksAndMetaDataAsString)

      pactEither.toOption.value shouldEqual PactFileExamples.simpleWithLinksAndMetaData
    }

    it("should be able to write Pact files and add metadata when missing") {

      val written = pactWriter.pactToJsonString(PactFileExamples.simple, scalaPactVersion)

      val expected = PactFileExamples.simpleWithMetaDataAsString

      parse(written).toOption.get shouldEqual parse(expected).toOption.get
    }
  }
}

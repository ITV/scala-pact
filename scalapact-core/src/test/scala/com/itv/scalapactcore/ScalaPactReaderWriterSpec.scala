package com.itv.scalapactcore

import org.scalatest.{FunSpec, Matchers}

class ScalaPactReaderWriterSpec extends FunSpec with Matchers {

  describe("Reading and writing a homogeneous Pact files") {

    it("should be able to read Pact files"){
      val pactEither = ScalaPactReader.jsonStringToPact(PactFileExamples.simpleAsString)

      pactEither.toOption.get shouldEqual PactFileExamples.simple
    }

    it("should be able to write Pact files"){

      val written = ScalaPactWriter.pactToJsonString(PactFileExamples.simple)

      val expected = PactFileExamples.simpleAsString

      written shouldEqual expected

    }

    it("should be able to eat it's own dog food"){

      val json = ScalaPactWriter.pactToJsonString(PactFileExamples.simple)

      val pact = ScalaPactReader.jsonStringToPact(json).toOption.get

      val `reJson'd` = ScalaPactWriter.pactToJsonString(pact)

      `reJson'd` shouldEqual PactFileExamples.simpleAsString
      pact shouldEqual PactFileExamples.simple

    }

  }

}


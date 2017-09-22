package com.itv.scalapact.shared
import org.scalatest.{FunSpec, Matchers}
import argonaut.JsonParser._

class ScalaPactReaderWriterSpec extends FunSpec with Matchers {

  import EitherWithToOption._

  describe("Reading and writing a homogeneous Pact files") {

    it("should be able to read Pact files") {
      val pactEither = PactReader.jsonStringToPact(PactFileExamples.simpleAsString)

      pactEither.right.get shouldEqual PactFileExamples.simple
    }

    it("should be able to read Pact files using the old provider state key") {
      val pactEither = PactReader.jsonStringToPact(PactFileExamples.simpleOldProviderStateAsString)

      pactEither.right.get shouldEqual PactFileExamples.simple
    }

    it("should be able to write Pact files") {

      val written = PactWriter.pactToJsonString(PactFileExamples.simple)

      val expected = PactFileExamples.simpleAsString

      parse(written).toOption.get shouldEqual parse(expected).toOption.get
    }

    it("should be able to eat it's own dog food") {

      val json = PactWriter.pactToJsonString(PactFileExamples.simple)

      val pact = PactReader.jsonStringToPact(json).right.get

      val `reJson'd` = parse(PactWriter.pactToJsonString(pact)).toOption.get

      `reJson'd` shouldEqual parse(PactFileExamples.simpleAsString).toOption.get

      pact shouldEqual PactFileExamples.simple

    }

    it("should be able to read ruby format json") {
      val pactEither = PactReader.jsonStringToPact(PactFileExamples.simpleAsString)

      pactEither.right.get shouldEqual PactFileExamples.simple
    }

    it("should be able to write a pact file in ruby format") {

      val written = PactWriter.pactToJsonString(PactFileExamples.simple)

      val expected = PactFileExamples.simpleAsString

      written shouldEqual expected

    }

    it("should be able to eat it's own dog food with no body") {

      val json = PactWriter.pactToJsonString(PactFileExamples.verySimple)

      val pact = PactReader.jsonStringToPact(json).right.get

      val `reJson'd` = parse(PactWriter.pactToJsonString(pact)).toOption.get

      `reJson'd` shouldEqual parse(PactFileExamples.verySimpleAsString).toOption.get

      pact shouldEqual PactFileExamples.verySimple

    }

    it("should be able to read ruby format json with no body") {
      val pactEither = PactReader.jsonStringToPact(PactFileExamples.verySimpleAsString)

      pactEither.right.get shouldEqual PactFileExamples.verySimple
    }

    it("should be able to write a pact file in ruby format with no body") {

      val written = PactWriter.pactToJsonString(PactFileExamples.verySimple)

      val expected = PactFileExamples.verySimpleAsString

      parse(written).toOption.get shouldEqual parse(expected).toOption.get
    }

  }

}

object EitherWithToOption {

  import scala.language.implicitConversions

  case class WithToOption[A, B](either: Either[A, B]) {
    def toOption: Option[B] =
      either match {
        case Right(r) =>
          Some(r)

        case Left(_) =>
          None
      }
  }

  implicit def toWithToOption[A, B](either: Either[A, B]): WithToOption[A, B] =
    WithToOption(either)
}

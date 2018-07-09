package com.itv.scalapact.circe09

import io.circe.parser.parse
import org.scalactic.TypeCheckedTripleEquals
import org.scalatest.Matchers._
import org.scalatest.{EitherValues, FlatSpec}

class ScalaMessagePactReaderWriterSpec extends FlatSpec with EitherValues with TypeCheckedTripleEquals {

  val pactReader = new PactReader
  val pactWriter = new PactWriter

  List(
    (PactFileExamples.stringMessageAsString, PactFileExamples.stringMessage),
    (PactFileExamples.jsonMessageAsString, PactFileExamples.jsonMessage),
    (PactFileExamples.multipleMessageAsString, PactFileExamples.multipleMessage),
    (PactFileExamples.multipleMessagesAndInteractionsAsString, PactFileExamples.multipleMessagesAndInteractions)
  ).foreach {
    case (expectedWritten, expectedPact) =>
      it should s"should be able to read Pact files: [$expectedPact]" in {

        val actualPact = pactReader.jsonStringToPact(expectedWritten)

        actualPact should ===(Right(expectedPact))
      }

      it should s"should be able to write Pact files: [$expectedPact]" in {

        val actualWritten = pactWriter.pactToJsonString(expectedPact)

        parse(actualWritten) should ===(parse(expectedWritten))

      }

      it should s"should be able to eat it's own dog food: [$expectedPact]" in {
        val actualWritten = pactWriter.pactToJsonString(expectedPact)

        val actualPact = pactReader.jsonStringToPact(actualWritten).right.value

        val `reJson'd` = pactWriter.pactToJsonString(actualPact)

        parse(`reJson'd`) should ===(parse(expectedWritten))

        actualPact should ===(expectedPact)
      }
  }

}

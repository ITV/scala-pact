package com.itv.scalapact.circe08

import org.scalactic.TypeCheckedTripleEquals
import org.scalatest.Matchers._
import org.scalatest.{EitherValues, FlatSpec}
import io.circe.parser.parse

class ScalaMessagePactReaderWriterSpec extends FlatSpec with EitherValues with TypeCheckedTripleEquals {

  val pactReader = new PactReader
  val pactWriter = new PactWriter

  it should "be able to read message that use 'providerState' instead 'providerStates'" in {
    val actualPact = pactReader.jsonStringToPact(PactFileExamples.simpleMessageWithProviderStateAsString)

    actualPact should ===(Right(PactFileExamples.stringMessage))

  }

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

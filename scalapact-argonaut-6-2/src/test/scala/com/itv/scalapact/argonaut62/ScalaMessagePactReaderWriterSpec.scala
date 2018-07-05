package com.itv.scalapact.argonaut62

import org.scalatest.Matchers._
import org.scalatest.{EitherValues, FlatSpec}
import argonaut.JsonParser.parse
import org.scalactic.TypeCheckedTripleEquals

class ScalaMessagePactReaderWriterSpec extends FlatSpec with EitherValues with TypeCheckedTripleEquals {

  val pactReader = new PactReader
  val pactWriter = new PactWriter

  it should "parse a simple message" in {
    val source = PactFileExamples.simpleMessageAsString
    val pact   = pactReader.jsonStringToPact(source).right.value

    val target = pactWriter.pactToJsonString(pact)

    parse(target) should ===(parse(source))

  }

}

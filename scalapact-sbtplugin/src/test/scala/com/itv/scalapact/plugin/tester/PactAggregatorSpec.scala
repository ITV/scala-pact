package com.itv.scalapact.plugin.tester

import java.io.File

import com.itv.scalapact.plugin.PactParser
import com.itv.scalapact.plugin.utils.FileManipulator
import com.itv.scalapactcore.{Interaction, Pact, PactActor}
import org.scalatest.{FlatSpec, Matchers}
import org.scalatest.mock.MockitoSugar


class PactAggregatorSpec extends FlatSpec with Matchers with MockitoSugar {

  import org.mockito.Mockito._

  val consumer = PactActor("someConsumer")
  val provider = PactActor("someProvider")
  val interaction1 = mock[Interaction]
  val interaction2 = mock[Interaction]
  val interaction3 = mock[Interaction]
  val interaction4 = mock[Interaction]
  val pact123 = new Pact(provider, consumer, List(interaction1, interaction2, interaction3))
  val pact4 = new Pact(provider, consumer, List(interaction4))
  val pact1234 = new Pact(provider, consumer, List(interaction1, interaction2, interaction3, interaction4))

  val file123 = new File("123")
  val file4 = new File("4")

  "The PactAggregator" should "load files, deleting them, and aggregate the result in a pact" in {
    val fileManipulator = mock[FileManipulator]
    val pactParser = mock[PactParser]

    val aggregator = PactAggregator(fileManipulator, pactParser)
    when(fileManipulator.loadFileAndDelete(file123)) thenReturn "json123"
    when(fileManipulator.loadFileAndDelete(file4)) thenReturn "json4"
    when(pactParser.apply("json123")) thenReturn Some(pact123)
    when(pactParser.apply("json4")) thenReturn Some(pact4)

    aggregator(SquashDefn("someName", List(file123, file4))) shouldBe Some(pact1234)
  }

}

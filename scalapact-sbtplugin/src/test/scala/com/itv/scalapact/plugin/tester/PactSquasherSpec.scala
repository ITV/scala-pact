package com.itv.scalapact.plugin.tester

import com.itv.scalapactcore.Pact
import org.scalatest.mock.MockitoSugar
import org.scalatest.{FlatSpec, Matchers}


class PactSquasherSpec extends FlatSpec with Matchers with MockitoSugar with SquashedFixture {
import org.mockito.Mockito._
  "PactSquasher" should "load the pact files, deleting the originals, and produce an aggregate file" in {
    val aggregator = mock[PactAggregator]
    val pactFileWriter = mock[PactFileWriter]
    val squasher = new PactSquasher(aggregator, pactFileWriter)

    val pact = mock[Pact]
    when(aggregator.apply(squashDefn1)) thenReturn Some(pact)
    squasher(squashDefn1) shouldBe ()
    verify(pactFileWriter).apply(pact)
  }

}

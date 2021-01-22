package com.itv.scalapact

import org.scalatest.FlatSpec

class PactForgerSuiteSpec extends FlatSpec with PactForgerSuite {
  it should "run a test" in {
    forgePact
      .between("consumer")
      .and("provider")
      .addInteraction(
        interaction
          .description("")
          .uponReceiving(GET, "foo")
          .willRespondWith(200)
      )
      .runConsumerTest(_ => ())
  }

  it should "run another test" in {
    forgePact
      .between("consumer")
      .and("provider")
      .addInteraction(
        interaction
          .description("another test")
          .uponReceiving(GET, "bar")
          .willRespondWith(200)
      )
      .runConsumerTest(_ => ())
  }
}

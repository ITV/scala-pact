package com.itv.scalapact

//should compile
class PactForgerSuiteSpec extends PactForgerSuite {
  val pact =
    forgePact
      .between("consumer")
      .and("provider")
      .addInteraction(
        interaction
          .description("")
          .uponReceiving(GET, "")
          .willRespondWith(200)
      )
      .runConsumerTest(_ => ())
}

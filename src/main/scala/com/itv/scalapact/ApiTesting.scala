package com.itv.scalapact

object ApiTesting {

  def test(): Unit = {

    import ScalaPactBuilder._

    makePact.context("test schedule api")
      .consumer("goggons")
      .hasPactWith("dawkins query")
      .addInteraction(
        interaction.description("slots for production")
          .given("generdyd")
        .uponReceiving("GET", "/slots", Map(), None)
        .willRespondWith(200, Map(), None)
      )
      .runConsumerTest()


  }





}
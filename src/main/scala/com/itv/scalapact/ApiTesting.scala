package com.itv.scalapact

object ApiTesting {

  def test(): Unit = {

    import ScalaPactForger._

    forgePact
      .between("me")
      .and("him")
      .describing("an api call")
        .addInteraction(
          interaction
            .description("")
            .uponReceiving("/")
            .willRespondWith(200)
        )
        .addInteraction(
          interaction
            .description("")
            .uponReceiving(GET, "/fish")
            .willRespondWith(404, "dumbass")
        )
      .runConsumerTest(config => 1 == 1)

  }

}
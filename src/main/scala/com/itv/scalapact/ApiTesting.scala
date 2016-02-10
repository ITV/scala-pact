package com.itv.scalapact

object ApiTesting {

  def test(): Unit = {

    import ScalaPactForger._

    forgePact
      .between("me")
      .and("him")
      .describing("an api call")
      .runConsumerTest()

  }





}
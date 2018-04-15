package com.itv.scalapact

import cats.effect.IO
import com.itv.scalapact.http4s18.impl.{Http4sClientHelper, PactServer, ScalaPactHttpClient}
import com.itv.scalapact.shared.typeclasses.{IPactStubber, IScalaPactHttpClient}

package object http4s18 {

  implicit val serverInstance: IPactStubber =
    new PactServer

  implicit val scalaPactHttpClient: IScalaPactHttpClient[IO] =
    new ScalaPactHttpClient(Http4sClientHelper.doRequest)
}

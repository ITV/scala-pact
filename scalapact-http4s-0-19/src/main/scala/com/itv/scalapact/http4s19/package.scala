package com.itv.scalapact

import cats.effect.IO
import com.itv.scalapact.http4s18.impl.{Http4sClientHelper, PactServer, ScalaPactHttpClient}
import com.itv.scalapact.shared.typeclasses.{IPactStubber, IScalaPactHttpClient}

package object http4s19 {

  // Note that we create a new stubber anytime this implicit is needed (i.e. this is a `def`).
  // We need this because implementations of `IPactStubber` might want to have their own state about the server running.
  implicit def serverInstance: IPactStubber =
    new PactServer

  implicit val scalaPactHttpClient: IScalaPactHttpClient[IO] =
    new ScalaPactHttpClient(Http4sClientHelper.doRequest)
}

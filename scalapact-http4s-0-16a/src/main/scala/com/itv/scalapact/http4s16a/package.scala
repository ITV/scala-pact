package com.itv.scalapact

import com.itv.scalapact.http4s16a.impl.{Http4sClientHelper, PactServer, ScalaPactHttpClient}
import com.itv.scalapact.shared.typeclasses.{IPactStubber, IScalaPactHttpClient}
import scalaz.concurrent.Task

package object http4s16a {

  implicit val serverInstance: IPactStubber =
    new PactServer

  implicit val scalaPactHttpClient: IScalaPactHttpClient[Task] =
    new ScalaPactHttpClient(Http4sClientHelper.doRequest)
}

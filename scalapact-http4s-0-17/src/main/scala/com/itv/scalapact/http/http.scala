package com.itv.scalapact

import com.itv.scalapact.http4s17.impl.{Http4sClientHelper, PactServer, ScalaPactHttpClient}
import com.itv.scalapact.shared.typeclasses.{IPactStubber, IScalaPactHttpClient}
import fs2.Task

package object http {

  implicit val serverInstance: IPactStubber =
    new PactServer

  implicit val scalaPactHttpClient: IScalaPactHttpClient[Task] =
    new ScalaPactHttpClient(Http4sClientHelper.doRequest)
}

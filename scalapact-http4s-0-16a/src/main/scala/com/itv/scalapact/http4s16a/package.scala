package com.itv.scalapact

import com.itv.scalapact.shared.typeclasses.IScalaPactHttpClient
import scalaz.concurrent.Task

package object http4s16a {

//  implicit val serverInstance: IPactServer =
//    new PactServer(PactStubService.runServer())

  implicit val pactWriterInstance: IScalaPactHttpClient[Task] =
    new ScalaPactHttpClient
}

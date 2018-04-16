package com.itv.standalonestubber

import com.itv.scalapact.circe09.{pactReaderInstance, pactWriterInstance}
import com.itv.scalapact.http4s18.serverInstance
import com.itv.scalapact.shared.ColourOuput._
import com.itv.scalapact.shared.{PactLogger, ScalaPactSettings, SslContextMap}
import com.itv.scalapactcore.common.LocalPactFileLoader._
import com.itv.scalapactcore.common.stubber.InteractionManager

object PactStubber {

  def main(args: Array[String]): Unit = {

    PactLogger.message("*************************************".white.bold)
    PactLogger.message("** ScalaPact: Running Stubber      **".white.bold)
    PactLogger.message("*************************************".white.bold)

    val interactionManager: InteractionManager = new InteractionManager

    (ScalaPactSettings.parseArguments andThen loadPactFiles(pactReaderInstance)(true)("pacts") andThen interactionManager.addToInteractionManager andThen serverInstance
      .startServer(interactionManager, 5, sslContextName = None, port = None)(
        pactReaderInstance,
        pactWriterInstance,
        SslContextMap.defaultEmptyContextMap))(args)

    ()
  }

}

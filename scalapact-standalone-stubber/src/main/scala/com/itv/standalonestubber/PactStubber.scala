package com.itv.standalonestubber

import com.itv.scalapact.shared.ColourOuput._
import com.itv.scalapact.shared.{ScalaPactSettings, SslContextMap}
import com.itv.scalapactcore.common.LocalPactFileLoader._
import com.itv.scalapactcore.stubber.InteractionManager
import com.itv.scalapact.shared.http.PactStubService._
import com.itv.scalapactcore.common.PactReaderWriter._
import com.itv.scalapact.shared.PactLogger

object PactStubber {

  def main(args: Array[String]): Unit = {

    PactLogger.message("*************************************".white.bold)
    PactLogger.message("** ScalaPact: Running Stubber      **".white.bold)
    PactLogger.message("*************************************".white.bold)

    val interactionManager: InteractionManager = new InteractionManager

    (ScalaPactSettings.parseArguments andThen loadPactFiles(pactReader)(true)("pacts") andThen interactionManager.addToInteractionManager andThen startServer(interactionManager, sslContextName = None)(pactReader, pactWriter, implicitly[SslContextMap]))(args)

  }

}


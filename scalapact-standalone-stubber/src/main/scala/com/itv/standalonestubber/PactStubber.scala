package com.itv.standalonestubber

import com.itv.scalapact.http.serverInstance
import com.itv.scalapact.json.{pactReaderInstance, pactWriterInstance}
import com.itv.scalapact.shared.ColourOuput._
import com.itv.scalapact.shared.typeclasses.IPactStubber
import com.itv.scalapact.shared.{PactLogger, ScalaPactSettings}
import com.itv.scalapactcore.common.LocalPactFileLoader._
import com.itv.scalapactcore.common.stubber.InteractionManager

object PactStubber {

  def main(args: Array[String]): Unit = {

    PactLogger.message("*************************************".white.bold)
    PactLogger.message("** ScalaPact: Running Stubber      **".white.bold)
    PactLogger.message("*************************************".white.bold)

    val interactionManager = new InteractionManager

    val parseArgs       = ScalaPactSettings.parseArguments
    val loadPacts       = loadPactFiles(pactReaderInstance)(true)("pacts")
    val addInteractions = interactionManager.addToInteractionManager
    val startUp = serverInstance
      .startStubServer(interactionManager, 5, sslContextName = None, port = None)

    val launch: Seq[String] => IPactStubber =
      parseArgs andThen loadPacts andThen addInteractions andThen startUp

    launch(args)

    ()
  }

}

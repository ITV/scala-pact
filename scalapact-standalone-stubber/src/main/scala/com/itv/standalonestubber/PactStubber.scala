package com.itv.standalonestubber

import java.io.File
import java.util.ResourceBundle
import java.util.concurrent.{ExecutorService, Executors}

import com.ing.pact.stubber.ConfigBasedStubber
import com.itv.scalapact.shared.ColourOuput._
import com.itv.scalapact.shared.{Helpers, PactLogger, ScalaPactSettings, SslContextMap}
import com.itv.scalapactcore.common.LocalPactFileLoader._
import com.itv.scalapactcore.stubber.InteractionManager
import com.itv.scalapact.shared.http.PactStubService._
import com.itv.scalapactcore.common.PactReaderWriter._

object PactStubber {

  def main(args: Array[String]): Unit = {

    val argMap = Helpers.pair apply args.toList
    println(argMap)
    argMap.get("--file") match {
      case Some(fileName) =>
        implicit val resources: ResourceBundle = ResourceBundle.getBundle("messages")
        implicit val executorService: ExecutorService = Executors.newFixedThreadPool(10)
        new ConfigBasedStubber(new File(fileName)).waitForever()
      case None =>
        PactLogger.message("*************************************".white.bold)
        PactLogger.message("** ScalaPact: Running Stubber      **".white.bold)
        PactLogger.message("*************************************".white.bold)
        val interactionManager: InteractionManager = new InteractionManager
        (ScalaPactSettings.convertToArguments andThen loadPactFiles(pactReader)(true)("pacts") andThen interactionManager.addToInteractionManager andThen startServer(interactionManager, sslContextName = None)(pactReader, pactWriter, implicitly[SslContextMap])) (argMap)
    }
  }

}


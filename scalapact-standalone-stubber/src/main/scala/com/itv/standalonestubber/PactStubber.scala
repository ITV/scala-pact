package com.itv.standalonestubber

import java.util.concurrent.{ExecutorService, Executors}

import com.itv.scalapact.http.serverInstance
import com.itv.scalapact.json.{pactReaderInstance, pactWriterInstance}
import com.itv.scalapact.shared.ColourOutput._
import com.itv.scalapact.shared.typeclasses.IPactStubber
import com.itv.scalapact.shared.{PactLogger, ScalaPactSettings, SslContextMap}
import com.itv.scalapactcore.common.LocalPactFileLoader._
import com.itv.scalapactcore.common.stubber.InteractionManager

object PactStubber {

  private val pool: ExecutorService = Executors.newFixedThreadPool(1)

  def main(args: Array[String]): Unit = {

    PactLogger.message("*************************************".white.bold)
    PactLogger.message("** ScalaPact: Running Stubber      **".white.bold)
    PactLogger.message("*************************************".white.bold)

    val interactionManager = new InteractionManager

    val parseArgs       = ScalaPactSettings.parseArguments
    val loadPacts       = loadPactFiles(pactReaderInstance)(true)("pacts")
    val addInteractions = interactionManager.addToInteractionManager

    val startUp: ScalaPactSettings => IPactStubber = scalaPactSettings => {
      PactLogger.message(
        ("Starting ScalaPact Stubber on: http://" + scalaPactSettings.giveHost + ":" + scalaPactSettings.givePort.toString).white.bold
      )
      PactLogger.message(("Strict matching mode: " + scalaPactSettings.giveStrictMode.toString).white.bold)

      serverInstance.start(interactionManager, 5, sslContextName = None, port = None)(
        pactReaderInstance,
        pactWriterInstance,
        SslContextMap.defaultEmptyContextMap
      )(scalaPactSettings)
    }

    val launch: Seq[String] => IPactStubber = args => {
      val settings = parseArgs(args)
      addInteractions(loadPacts(settings))
      startUp(settings)
    }

    pool.execute(new RunnableStubber(args, launch))

    ()
  }

}

class RunnableStubber(args: Array[String], launch: Seq[String] => IPactStubber) extends Runnable {
  override def run(): Unit = {
    launch(args.toSeq)
    ()
  }
}

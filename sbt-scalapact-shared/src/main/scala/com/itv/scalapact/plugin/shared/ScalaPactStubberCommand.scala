package com.itv.scalapact.plugin.shared

import com.itv.scalapact.shared.ColourOutput._
import com.itv.scalapact.shared.typeclasses.{IPactReader, IPactStubber, IPactWriter}
import com.itv.scalapact.shared.{PactLogger, ScalaPactSettings, SslContextMap}
import com.itv.scalapactcore.common.LocalPactFileLoader._
import com.itv.scalapactcore.common.stubber.InteractionManager

import scala.io.StdIn

object ScalaPactStubberCommand {

  def interactionManagerInstance: InteractionManager = new InteractionManager

  def runStubber(scalaPactSettings: ScalaPactSettings, interactionManager: InteractionManager)(
      implicit pactReader: IPactReader,
      pactWriter: IPactWriter,
      pactStubber: IPactStubber,
      sslContextMap: SslContextMap
  ): Unit = {
    val loadPacts    = loadPactFiles(pactReader)(true)(scalaPactSettings.giveOutputPath)
    val addToManager = interactionManager.addToInteractionManager

    val launchStub: ScalaPactSettings => IPactStubber = setting => {
      PactLogger.message(
        ("Starting ScalaPact Stubber on: http://" + scalaPactSettings.giveHost + ":" + scalaPactSettings.givePort.toString).white.bold
      )
      PactLogger.message(("Strict matching mode: " + scalaPactSettings.giveStrictMode.toString).white.bold)

      pactStubber.start(interactionManager, 2, None, scalaPactSettings.port)(
        pactReader,
        pactWriter,
        sslContextMap
      )(setting)
    }

    @SuppressWarnings(Array("org.wartremover.warts.DiscardedNonUnitValue"))
    val launch: ScalaPactSettings => Unit = args => {
      addToManager(loadPacts(args))
      launchStub(args)
      ()
    }

    launch(scalaPactSettings)

    PactLogger.message("**Press ENTER to quit**".cyan.bold)

    StdIn.readLine()

    ()
  }

}

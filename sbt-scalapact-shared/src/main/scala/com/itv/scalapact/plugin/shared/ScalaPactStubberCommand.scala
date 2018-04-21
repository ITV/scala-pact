package com.itv.scalapact.plugin.shared

import com.itv.scalapact.shared.{ScalaPactSettings, SslContextMap}
import com.itv.scalapact.shared.typeclasses.{IPactReader, IPactStubber, IPactWriter}
import com.itv.scalapactcore.common.LocalPactFileLoader._
import com.itv.scalapactcore.common.stubber.InteractionManager

object ScalaPactStubberCommand {

  def interactionManagerInstance: InteractionManager = new InteractionManager

  def runStubber(scalaPactSettings: ScalaPactSettings, interactionManager: InteractionManager)(
      implicit pactReader: IPactReader,
      pactWriter: IPactWriter,
      pactStubber: IPactStubber,
      sslContextMap: SslContextMap
  ): IPactStubber = {
    val loadPacts    = loadPactFiles(pactReader)(true)(scalaPactSettings.giveOutputPath)
    val addToManager = interactionManager.addToInteractionManager
    val launchStub = pactStubber.startLongRunningStubServer(interactionManager, 2, None, scalaPactSettings.port)(
      pactReader,
      pactWriter,
      sslContextMap
    )

    (loadPacts andThen addToManager andThen launchStub)(scalaPactSettings)
  }

}

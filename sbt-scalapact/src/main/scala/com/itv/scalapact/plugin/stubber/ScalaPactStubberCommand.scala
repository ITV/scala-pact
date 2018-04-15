package com.itv.scalapact.plugin.stubber

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
      sslContextMap: SslContextMap): Unit = {
    (loadPactFiles(pactReader)(true)(scalaPactSettings.giveOutputPath) andThen interactionManager.addToInteractionManager andThen pactStubber
      .startServer(interactionManager, 2, None, None)(pactReader, pactWriter, sslContextMap))(scalaPactSettings)
    ()
  }

}

package com.itv.scalapact.plugin.stubber

import com.itv.scalapact.shared.ScalaPactSettings
import com.itv.scalapact.shared.typeclasses.{IPactReader, IPactStubber, IPactWriter}
import com.itv.scalapactcore.common.LocalPactFileLoader._
import com.itv.scalapactcore.common.stubber.InteractionManager

object ScalaPactStubberCommand {

  def interactionManagerInstance: InteractionManager = new InteractionManager

  def runStubber(scalaPactSettings: ScalaPactSettings, interactionManager: InteractionManager)(implicit pactReader: IPactReader, pactWriter: IPactWriter, pactStubber: IPactStubber): Unit = {
    (loadPactFiles(pactReader)(true)(scalaPactSettings.giveOutputPath) andThen interactionManager.addToInteractionManager andThen pactStubber.startServer(interactionManager, 2, None, scalaPactSettings.givePort)(pactReader, pactWriter))(scalaPactSettings)
    ()
  }

}
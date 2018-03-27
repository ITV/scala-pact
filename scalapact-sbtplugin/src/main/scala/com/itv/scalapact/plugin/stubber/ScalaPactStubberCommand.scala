package com.itv.scalapact.plugin.stubber

import com.itv.scalapact.shared.http.PactStubService._
import com.itv.scalapact.shared.typeclasses.{IPactReader, IPactWriter}
import com.itv.scalapact.shared.{ScalaPactSettings, SslContextMap}
import com.itv.scalapactcore.common.LocalPactFileLoader._
import com.itv.scalapactcore.stubber.InteractionManager

object ScalaPactStubberCommand {

  def interactionManagerInstance: InteractionManager = new InteractionManager

  def runStubber(scalaPactSettings: ScalaPactSettings, interactionManager: InteractionManager)(implicit pactReader: IPactReader, pactWriter: IPactWriter): Unit = {
    (loadPactFiles(pactReader)(true)(scalaPactSettings.giveOutputPath) andThen interactionManager.addToInteractionManager andThen startServer(interactionManager, sslContextName = None)(pactReader, pactWriter, implicitly[SslContextMap]))(scalaPactSettings)
  }

}
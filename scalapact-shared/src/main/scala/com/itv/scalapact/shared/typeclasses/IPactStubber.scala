package com.itv.scalapact.shared.typeclasses

import com.itv.scalapact.shared.{IInteractionManager, ScalaPactSettings}

trait IPactStubber {

  def startServer(interactionManager: IInteractionManager, connectionPoolSize: Int, sslContextName: Option[String], port: Option[Int])(implicit pactReader: IPactReader, pactWriter: IPactWriter): ScalaPactSettings => IPactStubber

  def awaitShutdown(): Unit

  def shutdown(): Unit

}

package com.itv.scalapact.shared.typeclasses

import com.itv.scalapact.shared.{IInteractionManager, ScalaPactSettings, SslContextMap}

trait IPactStubber {

  def startTestServer(interactionManager: IInteractionManager,
                      connectionPoolSize: Int,
                      sslContextName: Option[String],
                      port: Option[Int])(implicit pactReader: IPactReader,
                                         pactWriter: IPactWriter,
                                         sslContextMap: SslContextMap): ScalaPactSettings => IPactStubber

  def startStubServer(interactionManager: IInteractionManager,
                      connectionPoolSize: Int,
                      sslContextName: Option[String],
                      port: Option[Int])(implicit pactReader: IPactReader,
                                         pactWriter: IPactWriter,
                                         sslContextMap: SslContextMap): ScalaPactSettings => IPactStubber

  def awaitShutdown(): Unit

  def shutdown(): Unit

}

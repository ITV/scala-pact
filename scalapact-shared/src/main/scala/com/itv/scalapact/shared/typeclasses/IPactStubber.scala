package com.itv.scalapact.shared.typeclasses

import com.itv.scalapact.shared.{IInteractionManager, ScalaPactSettings, SslContextMap}

trait IPactStubber {

  def start(interactionManager: IInteractionManager,
            connectionPoolSize: Int,
            sslContextName: Option[String],
            port: Option[Int])(implicit pactReader: IPactReader,
                               pactWriter: IPactWriter,
                               sslContextMap: SslContextMap): ScalaPactSettings => IPactStubber

  def shutdown(): Unit

}

package com.itv.scalapact.shared

import com.itv.scalapact.shared.http.SslContextMap
import com.itv.scalapact.shared.json.{IPactReader, IPactWriter}
import com.itv.scalapact.shared.settings.ScalaPactSettings

trait IPactStubber {

  def start(
      interactionManager: IInteractionManager,
      connectionPoolSize: Int,
      sslContextName: Option[String],
      port: Option[Int]
  )(implicit
      pactReader: IPactReader,
      pactWriter: IPactWriter,
      sslContextMap: SslContextMap
  ): ScalaPactSettings => IPactStubber

  def shutdown(): Unit

  def port: Option[Int]
}

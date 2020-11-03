package com.itv.scalapact.shared

import com.itv.scalapact.shared.http.SslContextMap
import com.itv.scalapact.shared.json.{IPactReader, IPactWriter}

trait IPactStubber {

  def interactionManager: IInteractionManager

  def start(
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

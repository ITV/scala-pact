package com.itv.scalapact.http4s23.impl

import cats.effect._
import cats.effect.unsafe.implicits.global
import com.itv.scalapact.shared.http.SslContextMap
import com.itv.scalapact.shared.{IInteractionManager, IPactStubber, ScalaPactSettings}
import com.itv.scalapact.shared.json.{IPactReader, IPactWriter}
import org.http4s.blaze.server.BlazeServerBuilder

import scala.concurrent.Future

class PactStubber extends IPactStubber {

  private var instance: Option[() => Future[Unit]] = None
  private var _port: Option[Int]                   = None

  private def blazeServerBuilder(
      scalaPactSettings: ScalaPactSettings,
      interactionManager: IInteractionManager,
      connectionPoolSize: Int,
      sslContextName: Option[String],
      port: Option[Int]
  )(implicit pactReader: IPactReader, pactWriter: IPactWriter, sslContextMap: SslContextMap): BlazeServerBuilder[IO] =
    PactStubService.createServer(
      interactionManager,
      connectionPoolSize,
      sslContextName,
      port,
      scalaPactSettings
    )

  def start(
      interactionManager: IInteractionManager,
      connectionPoolSize: Int,
      sslContextName: Option[String],
      port: Option[Int]
  )(implicit
      pactReader: IPactReader,
      pactWriter: IPactWriter,
      sslContextMap: SslContextMap
  ): ScalaPactSettings => IPactStubber =
    scalaPactSettings => {
      instance match {
        case Some(_) =>
          this

        case None =>
          instance = Some(
            blazeServerBuilder(
              scalaPactSettings,
              interactionManager,
              connectionPoolSize,
              sslContextName,
              _port
            ).resource
              .use { server =>
                IO { _port = Some(server.address.getPort) } *> IO.never
              }
              .unsafeRunCancelable()
          )
          this
      }
    }

  def shutdown(): Unit = {
    instance.foreach(_())
    instance = None
    _port = None
  }

  def port: Option[Int] = _port
}

package com.itv.scalapact.http4s21.impl

import cats.effect._
import cats.implicits._
import com.itv.scalapact.shared.typeclasses.{IPactReader, IPactStubber, IPactWriter}
import com.itv.scalapact.shared.{IInteractionManager, ScalaPactSettings, SslContextMap}
import org.http4s.server.blaze.BlazeServerBuilder

import scala.concurrent.ExecutionContext

class PactStubber extends IPactStubber {

  private var instance: Option[CancelToken[IO]] = None
  private var _port: Option[Int] = None

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

  @SuppressWarnings(Array("org.wartremover.warts.Any"))
  def start(interactionManager: IInteractionManager,
            connectionPoolSize: Int,
            sslContextName: Option[String],
            port: Option[Int])(implicit pactReader: IPactReader,
                               pactWriter: IPactWriter,
                               sslContextMap: SslContextMap): ScalaPactSettings => IPactStubber =
    scalaPactSettings => {
      instance match {
        case Some(_) =>
          this

        case None =>
        implicit val cs: ContextShift[IO] = IO.contextShift(ExecutionContext.global)
          instance = Some(
            blazeServerBuilder(scalaPactSettings, interactionManager, connectionPoolSize, sslContextName, _port)
              .resource
              .use { server =>
                IO(_port = Some(server.address.getPort)) *> IO.never
              }
              .runCancelable(_ => IO.unit)
              .unsafeRunSync()
          )
          this
      }
    }

  @SuppressWarnings(Array("org.wartremover.warts.Any"))
  def shutdown(): Unit = {
    instance.foreach(_.unsafeRunSync())
    instance = None
    _port = None
  }

  def port: Option[Int] = _port
}

package com.itv.scalapact.http4s23.impl

import cats.effect._
import cats.effect.unsafe.implicits.global
import com.itv.scalapact.shared.http.SslContextMap
import com.itv.scalapact.shared.{IInteractionManager, IPactStubber, ScalaPactSettings}
import com.itv.scalapact.shared.json.{IPactReader, IPactWriter}
import org.http4s.blaze.server.BlazeServerBuilder

private final case class PortAndShutdownTask(port: Int, shutdown: IO[Unit])

class PactStubber extends IPactStubber {

  private var instance: Option[PortAndShutdownTask] = None

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
            PortAndShutdownTask.tupled(
              blazeServerBuilder(
                scalaPactSettings,
                interactionManager,
                connectionPoolSize,
                sslContextName,
                instance.map(_.port)
              ).resource.map(_.address.getPort).allocated.unsafeRunSync()
            )
          )
          this
      }
    }

  def shutdown(): Unit = {
    instance.foreach(_.shutdown.unsafeRunSync())
    instance = None
  }

  def port: Option[Int] = instance.map(_.port)
}

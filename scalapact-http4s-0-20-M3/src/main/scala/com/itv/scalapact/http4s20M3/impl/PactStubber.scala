package com.itv.scalapact.http4s20M3.impl
import cats.effect.{IO, Resource}
import com.itv.scalapact.shared.{IInteractionManager, ScalaPactSettings, SslContextMap}
import com.itv.scalapact.shared.typeclasses.{IPactReader, IPactStubber, IPactWriter}
import org.http4s.server.Server
import org.http4s.server.blaze.BlazeServerBuilder

class PactStubber extends IPactStubber {

  private var instance: Option[Resource[IO, Server[IO]]] = None

  private def blazeBuilder(
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
          instance = Option(
            blazeBuilder(scalaPactSettings, interactionManager, connectionPoolSize, sslContextName, port).resource
          )
          this
      }
    }

  def shutdown(): Unit = {
    instance = None
  }

  def port: Option[Int] = instance.map(_.use(s => IO(s.address.getPort)).unsafeRunSync())
}

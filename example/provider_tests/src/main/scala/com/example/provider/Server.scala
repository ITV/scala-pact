package com.example.provider

import org.http4s.server.blaze.BlazeBuilder
import org.http4s.server._

object AlternateStartupApproach {

  def startServer(): Server = {
    BlazeBuilder.bindHttp(8080)
      .mountService(Provider.service, "/")
      .run
    }

  def stopServer(server: Server): Unit = {
    server.shutdown
  }

  // On ordinary start up, this service will begin here.
  def main(args: Array[String]): Unit = {
    startServer().awaitShutdown()
  }

}

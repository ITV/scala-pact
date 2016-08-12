package com.example.provider

import org.http4s.server.blaze.BlazeBuilder

object Server extends App {
  BlazeBuilder.bindHttp(8080)
    .mountService(Provider.service, "/")
    .run
    .awaitShutdown()
}

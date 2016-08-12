package com.example

import org.http4s.server.blaze.BlazeBuilder

object BlazeExample extends App {
  BlazeBuilder.bindHttp(8080)
    .mountService(HelloWorld.service, "/")
    .run
    .awaitShutdown()
}

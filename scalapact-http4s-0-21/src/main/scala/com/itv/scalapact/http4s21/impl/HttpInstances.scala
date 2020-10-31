package com.itv.scalapact.http4s21.impl

import com.itv.scalapact.shared.IPactStubber
import com.itv.scalapact.shared.http.{IScalaPactHttpClientBuilder, SslContextMap}

import scala.concurrent.duration.Duration

trait HttpInstances {

  // Note that we create a new stubber anytime this implicit is needed (i.e. this is a `def`).
  // We need this because implementations of `IPactStubber` might want to have their own state about the server running.
  implicit def serverInstance: IPactStubber =
    new PactStubber

  implicit def httpClientBuilder(implicit sslContextMap: SslContextMap): IScalaPactHttpClientBuilder =
    (clientTimeout: Duration, sslContextName: Option[String], maxTotalConnections: Int) =>
      ScalaPactHttpClient(clientTimeout, sslContextName, maxTotalConnections)
}

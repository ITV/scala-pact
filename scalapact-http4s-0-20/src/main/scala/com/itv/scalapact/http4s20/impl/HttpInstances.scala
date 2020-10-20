package com.itv.scalapact.http4s20.impl

import cats.effect.IO
import com.itv.scalapact.shared.typeclasses.{IPactStubber, IResultPublisherBuilder, IScalaPactHttpClientBuilder}
import com.itv.scalapact.shared.SslContextMap

import scala.concurrent.duration.Duration

trait HttpInstances {

  // Note that we create a new stubber anytime this implicit is needed (i.e. this is a `def`).
  // We need this because implementations of `IPactStubber` might want to have their own state about the server running.
  implicit def serverInstance: IPactStubber =
    new PactStubber

  implicit def httpClientBuilder(implicit sslContextMap: SslContextMap): IScalaPactHttpClientBuilder[IO] =
    (clientTimeout: Duration, sslContextName: Option[String]) => ScalaPactHttpClient(clientTimeout, sslContextName)

  implicit def resultPublisherBuilder(implicit sslContextMap: SslContextMap): IResultPublisherBuilder =
    (clientTimeout: Duration, sslContextName: Option[String]) => ResultPublisher(clientTimeout, sslContextName)
}

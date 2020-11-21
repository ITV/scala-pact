package com.itv.scalapact.http4s21.impl

import java.util.concurrent.ConcurrentHashMap

import com.itv.scalapact.http4s21.impl.HttpInstances.ClientConfig
import com.itv.scalapact.shared.IPactStubber
import com.itv.scalapact.shared.http.{IScalaPactHttpClient, IScalaPactHttpClientBuilder, SslContextMap}
import com.itv.scalapact.shared.utils.PactLogger

import scala.concurrent.duration.Duration

trait HttpInstances {

  // Note that we create a new stubber anytime this implicit is needed (i.e. this is a `def`).
  // We need this because implementations of `IPactStubber` might want to have their own state about the server running.
  implicit def serverInstance: IPactStubber =
    new PactStubber

  private val clients: ConcurrentHashMap[ClientConfig, IScalaPactHttpClient] = new ConcurrentHashMap

  implicit def httpClientBuilder(implicit sslContextMap: SslContextMap): IScalaPactHttpClientBuilder = {
    (clientTimeout: Duration, sslContextName: Option[String], maxTotalConnections: Int) =>
      val clientConfig = ClientConfig(clientTimeout, sslContextName, maxTotalConnections, sslContextMap)
      PactLogger.debug(s"Checking client cache for config $clientConfig, cache size is ${clients.size}")
      clients.computeIfAbsent(
        clientConfig,
        _ => ScalaPactHttpClient(clientTimeout, sslContextName, maxTotalConnections)
      )
  }
}

object HttpInstances {
  private final case class ClientConfig(
      timeout: Duration,
      sslContextName: Option[String],
      maxTotalConnections: Int,
      sslContextMap: SslContextMap
  )
}

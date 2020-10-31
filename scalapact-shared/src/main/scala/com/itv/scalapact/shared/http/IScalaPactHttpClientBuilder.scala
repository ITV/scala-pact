package com.itv.scalapact.shared.http

import scala.concurrent.duration.{Duration, DurationInt}

trait IScalaPactHttpClientBuilder {
  def build(clientTimeout: Duration, sslContextName: Option[String], maxTotalConnections: Int): IScalaPactHttpClient

  def buildWithDefaults(clientTimeout: Option[Duration], sslContextName: Option[String]): IScalaPactHttpClient =
    build(clientTimeout.getOrElse(2.seconds), sslContextName, 1)
}

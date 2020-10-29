package com.itv.scalapact.shared.typeclasses

import scala.concurrent.duration.{Duration, DurationInt}

trait IScalaPactHttpClientBuilder[F[_]] {
  def build(clientTimeout: Duration, sslContextName: Option[String]): IScalaPactHttpClient[F]

  def buildWithDefaults(clientTimeout: Option[Duration], sslContextName: Option[String]): IScalaPactHttpClient[F] =
    build(clientTimeout.getOrElse(2.seconds), sslContextName)
}

package com.itv.scalapact.shared.typeclasses

import scala.concurrent.duration.Duration

trait IScalaPactHttpClientBuilder[F[_]] {
  def build(clientTimeout: Duration, sslContextName: Option[String]): IScalaPactHttpClient[F]
}

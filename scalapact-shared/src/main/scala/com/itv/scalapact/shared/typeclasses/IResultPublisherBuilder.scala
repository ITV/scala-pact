package com.itv.scalapact.shared.typeclasses

import scala.concurrent.duration.Duration

trait IResultPublisherBuilder {
  def build(clientTimeout: Duration, sslContextName: Option[String]): IResultPublisher
}

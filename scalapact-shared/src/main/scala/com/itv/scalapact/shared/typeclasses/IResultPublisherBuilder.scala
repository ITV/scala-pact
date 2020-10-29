package com.itv.scalapact.shared.typeclasses

import scala.concurrent.duration.{Duration, DurationInt}

trait IResultPublisherBuilder {
  def build(clientTimeout: Duration, sslContextName: Option[String]): IResultPublisher
  def buildWithDefaults(clientTimeout: Option[Duration], sslContextName: Option[String]): IResultPublisher =
    build(clientTimeout.getOrElse(2.seconds), sslContextName)
}

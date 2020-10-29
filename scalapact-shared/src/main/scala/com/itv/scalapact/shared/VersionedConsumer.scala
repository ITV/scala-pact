package com.itv.scalapact.shared

import java.net.URLEncoder

sealed abstract case class VersionedConsumer(name: String, versionUrlPart: VersionUrlPart)

final case class VersionUrlPart(value: String)

object VersionedConsumer {
  def apply(name: String, version: String): VersionedConsumer = new VersionedConsumer(name, VersionUrlPart(s"/version/$version")) {}
  def fromName(name: String): VersionedConsumer = new VersionedConsumer(name, VersionUrlPart("/latest")) {}
  def fromNameAndTags(name: String, tags: List[String]): List[VersionedConsumer] =
    tags.map(t => new VersionedConsumer(name, VersionUrlPart(s"/latest/${URLEncoder.encode(t, "UTF-8")}")) {})
}

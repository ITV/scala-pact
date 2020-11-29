package com.itv.scalapact.shared

final case class TaggedConsumer(name: String, tags: List[String]) {
  def toVersionedConsumers: List[VersionedConsumer] = VersionedConsumer.fromNameAndTags(name, tags)
}

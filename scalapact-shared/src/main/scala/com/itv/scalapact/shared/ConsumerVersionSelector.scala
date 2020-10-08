package com.itv.scalapact.shared

final case class ConsumerVersionSelector(tag: String, fallbackTag: Option[String], consumer: Option[String], latest: Option[Boolean]) //TODO check whether this can be non-optional

object ConsumerVersionSelector {
  //A version selector without a `"latest": true` indicates that ALL of the versions for that tag should be selected
  def apply(tag: String): ConsumerVersionSelector = ConsumerVersionSelector(tag, None, None, None)
  def apply(tag: String, latest: Boolean): ConsumerVersionSelector =
    ConsumerVersionSelector(tag, None, None, if (latest) Some(latest) else None)
}
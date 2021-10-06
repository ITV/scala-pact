package com.itv.scalapact.shared

/*
consumerVersionSelectors.tag: the tag name(s) of the consumer versions to get the pacts for.

consumerVersionSelectors.fallbackTag: the name of the tag to fallback to if the specified tag does not exist.
This is useful when the consumer and provider use matching branch names to coordinate the development of new features.

consumerVersionSelectors.latest: true. If the latest flag is omitted, all the pacts with the specified tag will be returned.
(This might seem a bit weird, but it's done this way to match the syntax used for the matrix query params. See https://docs.pact.io/selectors)

consumerVersionSelectors.consumer: allows a selector to only be applied to a certain consumer.
This is used when there is an API that has multiple consumers, one of which is a deployed service, and one of which is a mobile consumer.
The deployed service only needs the latest production pact verified, where as the mobile consumer may want all the production pacts verified.
 */

final case class ConsumerVersionSelector(
    tag: String,
    fallbackTag: Option[String],
    consumer: Option[String],
    latest: Option[Boolean],
    deployedOrReleased: Option[Boolean],
    deployed: Option[Boolean],
    released: Option[Boolean],
    environment: Option[String]
) {
  def withFallbackTag(tag: String): ConsumerVersionSelector          = copy(fallbackTag = Some(tag))
  def withConsumer(consumer: String): ConsumerVersionSelector        = copy(consumer = Some(consumer))
  def withLatest(latest: Boolean): ConsumerVersionSelector           = copy(latest = if (latest) Some(latest) else None)
  def withDeployedOrReleased(dOrR: Boolean): ConsumerVersionSelector = copy(deployedOrReleased = Some(dOrR))
  def withDeployed(deployed: Boolean): ConsumerVersionSelector       = copy(deployed = Some(deployed))
  def withReleased(released: Boolean): ConsumerVersionSelector       = copy(released = Some(released))
  def withEnvironment(env: String): ConsumerVersionSelector          = copy(environment = Some(env))
}

object ConsumerVersionSelector {
  def apply(tag: String): ConsumerVersionSelector = ConsumerVersionSelector(tag, latest = false)
  def apply(tag: String, latest: Boolean): ConsumerVersionSelector =
    ConsumerVersionSelector(tag, None, None, if (latest) Some(latest) else None, None, None, None, None)
}

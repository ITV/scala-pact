package com.itv.scalapact.shared

final case class PactsForVerificationRequest(consumerVersionSelectors: List[ConsumerVersionSelector], providerVersionTags: List[String])

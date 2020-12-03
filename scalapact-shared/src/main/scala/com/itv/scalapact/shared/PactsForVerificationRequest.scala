package com.itv.scalapact.shared

import java.time.OffsetDateTime

final case class PactsForVerificationRequest(
    consumerVersionSelectors: List[ConsumerVersionSelector],
    providerVersionTags: List[String],
    includePendingStatus: Boolean,
    includeWipPactsSince: Option[OffsetDateTime]
)

package com.itv.scalapact.shared.settings

import java.time.OffsetDateTime

sealed trait PendingPactSettings {
  def enablePending: Boolean
  def includeWipPactsSince: Option[OffsetDateTime]
}

object PendingPactSettings {

  case object PendingDisabled extends PendingPactSettings {
    val enablePending: Boolean                       = false
    val includeWipPactsSince: Option[OffsetDateTime] = None
  }

  case object PendingEnabled extends PendingPactSettings {
    val enablePending: Boolean                       = true
    val includeWipPactsSince: Option[OffsetDateTime] = None
  }

  case class IncludeWipPacts(wipPactsSince: OffsetDateTime) extends PendingPactSettings {
    val enablePending: Boolean                       = true
    val includeWipPactsSince: Option[OffsetDateTime] = Some(wipPactsSince)
  }
}

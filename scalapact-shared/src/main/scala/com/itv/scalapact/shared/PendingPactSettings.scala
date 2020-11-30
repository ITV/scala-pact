package com.itv.scalapact.shared

import java.time.OffsetDateTime

sealed abstract case class PendingPactSettings(
    enablePending: Option[Boolean],
    includeWipPactsSince: Option[OffsetDateTime]
) {
  def append(that: PendingPactSettings): PendingPactSettings = that match {
    case PendingPactSettings(_, Some(wip))   => new PendingPactSettings(Some(true), Some(wip)) {}
    case PendingPactSettings(Some(false), _) => PendingPactSettings(Some(false))
    case PendingPactSettings(ep, wip) =>
      new PendingPactSettings(ep.orElse(this.enablePending), wip.orElse(this.includeWipPactsSince)) {}
  }
}

object PendingPactSettings {
  def empty: PendingPactSettings = new PendingPactSettings(None, None) {}
  def apply(enablePending: Option[Boolean], includeWipPactsSince: Option[OffsetDateTime]): PendingPactSettings =
    includeWipPactsSince match {
      case Some(wipSince) => apply(wipSince)
      case None           => apply(enablePending: Option[Boolean])
    }
  def apply(enablePending: Option[Boolean]): PendingPactSettings = new PendingPactSettings(enablePending, None) {}
  def apply(wipPactsSince: OffsetDateTime): PendingPactSettings =
    new PendingPactSettings(Some(true), Some(wipPactsSince)) {}
}

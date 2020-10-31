package com.itv.scalapact.shared

final case class ProviderStateResult(result: Boolean, modifyRequest: InteractionRequest => InteractionRequest)

object ProviderStateResult {
  def apply(): ProviderStateResult                = new ProviderStateResult(false, identity[InteractionRequest])
  def apply(result: Boolean): ProviderStateResult = new ProviderStateResult(result, identity[InteractionRequest])

  type SetupProviderState = String => ProviderStateResult
}

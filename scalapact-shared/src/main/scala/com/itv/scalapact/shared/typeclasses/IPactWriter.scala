package com.itv.scalapact.shared.typeclasses

import com.itv.scalapact.shared.{Pact, PactsForVerificationRequest}

trait IPactWriter {
  def pactToJsonString(pact: Pact, scalaPactVersion: String): String
  def pactsForVerificationRequestToJsonString(request: PactsForVerificationRequest): String
}

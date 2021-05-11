package com.itv.scalapact.shared.json

import com.itv.scalapact.shared.{Contract, PactsForVerificationRequest}

trait IPactWriter {
  def pactToJsonString(pact: Contract, scalaPactVersion: String): String

  def pactsForVerificationRequestToJsonString(request: PactsForVerificationRequest): String
}

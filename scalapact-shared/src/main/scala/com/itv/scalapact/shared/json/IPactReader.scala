package com.itv.scalapact.shared.json

import com.itv.scalapact.shared.{HALIndex, Pact, PactsForVerificationResponse}

trait IPactReader extends IJsonConversionFunctions {
  def jsonStringToPact(json: String): Either[String, Pact]

  def jsonStringToPactsForVerification(json: String): Either[String, PactsForVerificationResponse]

  def jsonStringToHALIndex(json: String): Either[String, HALIndex]
}

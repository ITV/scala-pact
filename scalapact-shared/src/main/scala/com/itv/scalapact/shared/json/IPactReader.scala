package com.itv.scalapact.shared.json

import com.itv.scalapact.shared.{HALIndex, JvmPact, Pact, PactsForVerificationResponse}

trait IPactReader extends IJsonConversionFunctions {

  def jsonStringToScalaPact(json: String): Either[String, Pact]

  def jsonStringToJvmPact(json: String): Either[String, JvmPact]

  def jsonStringToPactsForVerification(json: String): Either[String, PactsForVerificationResponse]

  def jsonStringToHALIndex(json: String): Either[String, HALIndex]
}

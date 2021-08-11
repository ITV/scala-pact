package com.itv.scalapact.circe14

import com.itv.scalapact.shared.{JvmPact, Contract, PactMetaData, PactsForVerificationRequest, Pact, VersionMetaData}
import com.itv.scalapact.shared.json.IPactWriter
import io.circe.syntax._

class PactWriter extends IPactWriter {
  import PactImplicits._

  def pactToJsonString(pact: Contract, scalaPactVersion: String): String =
    pact match {
      case p: JvmPact => p.asJson.spaces2
      case p: Pact =>
        val updatedMetaData: Option[PactMetaData] =
          p.metadata.orElse {
            Option(
              PactMetaData(
                pactSpecification = Option(VersionMetaData("2.0.0")), //TODO: Where to get this value from?
                `scala-pact` = Option(VersionMetaData(scalaPactVersion))
              )
            )
          }
        p.copy(metadata = updatedMetaData).asJson.deepDropNullValues.spaces2
    }

  def pactsForVerificationRequestToJsonString(request: PactsForVerificationRequest): String =
    request.asJson.deepDropNullValues.spaces2
}

package com.itv.scalapact.circe08

import com.itv.scalapact.shared.typeclasses.IPactWriter
import com.itv.scalapact.shared.{ConsumerVersionSelector, Pact, PactMetaData, VersionMetaData}
import io.circe.Printer
import io.circe.syntax._

class PactWriter extends IPactWriter {
  import PactImplicits._

  def pactToJsonString(pact: Pact, scalaPactVersion: String): String = {
    val updatedMetaData: Option[PactMetaData] =
      pact.metadata.orElse {
        Option(
          PactMetaData(
            pactSpecification = Option(VersionMetaData("2.0.0")), //TODO: Where to get this value from?
            `scala-pact` = Option(VersionMetaData(scalaPactVersion))
          )
        )
      }

    pact.copy(metadata = updatedMetaData).asJson.pretty(Printer.spaces2.copy(dropNullKeys = true))
  }

  override def consumerVersionSelectorsToJsonString(selectors: List[ConsumerVersionSelector], providerVersionTags: List[String]): String = ???
}

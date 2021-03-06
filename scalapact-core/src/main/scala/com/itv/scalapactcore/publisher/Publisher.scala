package com.itv.scalapactcore.publisher

import com.itv.scalapact.shared.{Contract, JvmPact, Pact, PactPublishSettings, ScalaPactSettings}
import com.itv.scalapact.shared.http.IScalaPactHttpClientBuilder
import com.itv.scalapact.shared.json.{IPactReader, IPactWriter}
import com.itv.scalapact.shared.utils.ColourOutput.ColouredString
import com.itv.scalapactcore.common.{LocalPactFileLoader, PactBrokerClient}

class Publisher(pactBrokerClient: PactBrokerClient)(implicit pactReader: IPactReader) {
  def publishPacts(
      pactPublishSettings: PactPublishSettings,
      scalaPactSettings: ScalaPactSettings
  ): List[PublishResult] = {
    val pacts: List[Contract] = {
      if (pactPublishSettings.isScalaPactContract)
        LocalPactFileLoader.loadPactFiles[Pact](allowTmpFiles = false, scalaPactSettings.giveOutputPath)(
          scalaPactSettings
        )
      else
        LocalPactFileLoader.loadPactFiles[JvmPact](allowTmpFiles = false, scalaPactSettings.giveOutputPath)(
          scalaPactSettings
        )
    }
    pactBrokerClient.publishPacts(pacts, pactPublishSettings)
  }
}

object Publisher {
  def apply(implicit
      httpClientBuilder: IScalaPactHttpClientBuilder,
      pactWriter: IPactWriter,
      pactReader: IPactReader
  ): Publisher = new Publisher(new PactBrokerClient)
}

sealed abstract class PublishResult(val isSuccess: Boolean) {
  def renderAsString: String
}
final case class PublishSuccess(context: String) extends PublishResult(true) {
  val renderAsString: String =
    s"""${context.yellow}
       |${"Success".green}
     """.stripMargin
}
final case class PublishFailed(context: String, message: String) extends PublishResult(false) {
  val renderAsString: String =
    s"""${context.yellow}
       |$message
     """.stripMargin
}

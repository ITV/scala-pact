package com.itv.scalapactcore.publisher

import com.itv.scalapact.shared.ColourOutput.ColouredString
import com.itv.scalapact.shared.typeclasses.{IPactReader, IPactWriter, IScalaPactHttpClientBuilder}
import com.itv.scalapact.shared.{PactPublishSettings, ScalaPactSettings}
import com.itv.scalapactcore.common.{LocalPactFileLoader, PactBrokerClient}

class Publisher(pactBrokerClient: PactBrokerClient)(implicit pactReader: IPactReader) {
  def publishPacts(pactPublishSettings: PactPublishSettings, scalaPactSettings: ScalaPactSettings): List[PublishResult] = {
    val pacts = LocalPactFileLoader.loadPactFiles(pactReader)(false)(scalaPactSettings.giveOutputPath)(scalaPactSettings)
    pactBrokerClient.publishPacts(pacts, pactPublishSettings)
  }
}

object Publisher {
  def apply(
    implicit httpClientBuilder: IScalaPactHttpClientBuilder,
    pactWriter: IPactWriter,
    pactReader: IPactReader): Publisher = new Publisher(new PactBrokerClient)
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

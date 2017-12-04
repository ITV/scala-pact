package com.itv.scalapact.plugin.publish

import com.itv.scalapact.shared._
import com.itv.scalapact.shared.ColourOuput._
import com.itv.scalapactcore.verifier.ValidatedDetails

object Publisher {

  def publishToBroker(sendIt: SimpleRequest => Either[Throwable, SimpleResponse], pactBrokerAddress: String, versionToPublishAs: String)(implicit pactWriter: IPactWriter): ConfigAndPacts => List[PublishResult] = configAndPacts =>
    configAndPacts.pacts.map { pact =>
      publishPact(sendIt, pact, versionToPublishAs) {
        ValidatedDetails.buildFrom(pact.consumer.name, pact.provider.name, pactBrokerAddress, "/latest")
      }
    }

  def publishPact(sendIt: SimpleRequest => Either[Throwable, SimpleResponse], pact: Pact, versionToPublishAs: String)(details: Either[String, ValidatedDetails])(implicit pactWriter: IPactWriter): PublishResult =
    details match {
      case Left(l) =>
        PublishFailed("Validation error", l)

      case Right(v) =>
        val address = v.validatedAddress.address + "/pacts/provider/" + v.providerName + "/consumer/" + v.consumerName + "/version/" + versionToPublishAs.replace("-SNAPSHOT", ".x")

        val context = s"Publishing '${v.consumerName} -> ${v.providerName}' to:\n > $address".yellow

        sendIt(SimpleRequest(address, "", HttpMethod.PUT, Map("Content-Type" -> "application/json"), Option(pactWriter.pactToJsonString(pact)), sslContextName = None)) match {
          case Right(r) if r.is2xx =>
            PublishSuccess(context)

          case Right(r) =>
            PublishFailed(context, s"$r\nFailed: ${r.statusCode}, ${r.body}".red)

          case Left(e) =>
            PublishFailed(context, s"Failed with error: ${e.getMessage}".red)
        }
    }

}

sealed trait PublishResult {
  val renderAsString: String
}
case class PublishSuccess(context: String) extends PublishResult {
  val renderAsString: String =
    s"""${context.yellow}
       |${"Success".green}
     """.stripMargin
}
case class PublishFailed(context: String, message: String) extends PublishResult {
  val renderAsString: String =
    s"""${context.yellow}
       |$message
     """.stripMargin
}

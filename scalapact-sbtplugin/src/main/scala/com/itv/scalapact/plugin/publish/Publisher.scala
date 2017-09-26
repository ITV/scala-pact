package com.itv.scalapact.plugin.publish

import com.itv.scalapact.shared._
import com.itv.scalapact.shared.ColourOuput._
import com.itv.scalapactcore.common._

import RightBiasEither._

object Publisher {

  private final case class ValidatedDetails(validatedAddress: String, providerName: String, consumerName: String)

  def publishToBroker(sendIt: SimpleRequest => Either[Throwable, SimpleResponse], pactBrokerAddress: String, versionToPublishAs: String)(implicit pactWriter: IPactWriter): ConfigAndPacts => List[PublishResult] = configAndPacts => {

    configAndPacts.pacts.map { pact =>
      publishPact(sendIt, pact, versionToPublishAs) {
        for {
          consumerName     <- Helpers.urlEncode(pact.consumer.name)
          providerName     <- Helpers.urlEncode(pact.provider.name)
          validatedAddress <- PactBrokerAddressValidation.checkPactBrokerAddress(pactBrokerAddress)
        } yield ValidatedDetails(validatedAddress.address, providerName, consumerName)
      }
    }

  }

  def publishPact(sendIt: SimpleRequest => Either[Throwable, SimpleResponse], pact: Pact, versionToPublishAs: String)(details: Either[String, ValidatedDetails])(implicit pactWriter: IPactWriter): PublishResult =
    details match {
      case Left(l) =>
        PublishFailed("Validation error", l)

      case Right(v) =>
        val address = v.validatedAddress + "/pacts/provider/" + v.providerName + "/consumer/" + v.consumerName + "/version/" + versionToPublishAs.replace("-SNAPSHOT", ".x")

        val context = s"Publishing '${v.consumerName} -> ${v.providerName}' to:\n > $address".yellow

        sendIt(SimpleRequest(address, "", HttpMethod.PUT, Map("Content-Type" -> "application/json"), Option(pactWriter.pactToJsonString(pact)))) match {
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

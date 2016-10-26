package com.itv.scalapact.plugin.publish

import com.itv.scalapactcore.common.ColourOuput._
import com.itv.scalapactcore.common._
import com.itv.scalapactcore.ScalaPactWriter

object Publisher {

  private final case class ValidatedDetails(validatedAddress: String, providerName: String, consumerName: String)

  lazy val publishToBroker: String => String => ConfigAndPacts => Unit = pactBrokerAddress => versionToPublishAs => configAndPacts => {

    configAndPacts.pacts.foreach { pact =>
      val details: Either[String, ValidatedDetails] =
        Helpers.urlEncode(pact.consumer.name) match {
          case Right(consumerName) =>
            Helpers.urlEncode(pact.provider.name) match {
              case Right(providerName) =>
                PactBrokerAddressValidation.checkPactBrokerAddress(pactBrokerAddress) match {
                  case Right(validatedAddress) =>
                    Right(ValidatedDetails(validatedAddress, providerName, consumerName))
                  case Left(l) => Left(l)
                }
              case Left(l) => Left(l)
            }
          case Left(l) => Left(l)
        }

      details match {
        case Left(l) =>
          println(l.red)

        case Right(v) =>
          //Not sure how I feel about this. Should you be able to publish snapshots? Pact broker will return these with a call to `/latest` ...
          val address = v.validatedAddress + "/pacts/provider/" + v.providerName + "/consumer/" + v.consumerName + "/version/" + versionToPublishAs.replace("-SNAPSHOT", ".x")

          println(s"Publishing to: $address".yellow)

          ScalaPactHttp.doRequest(ScalaPactHttp.PUT, address, "", Map("Content-Type" -> "application/json"), Option(ScalaPactWriter.pactToJsonString(pact))).unsafePerformSyncAttempt.toEither match {
            case Right(r) if r.is2xx => println("Success".green)
            case Right(r) =>
              println(r)
              println(s"Failed: ${r.statusCode}, ${r.body}".red)
            case Left(e) =>
              println(s"Failed with error: ${e.getMessage}".red)
          }
      }
    }

    Unit
  }
}

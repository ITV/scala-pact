package com.itv.scalapact.plugin.publish

import com.itv.scalapact.plugin.{HttpService, PactFormatter, PublishAddressGenerator}
import com.itv.scalapactcore.common.ColourOuput._
import com.itv.scalapactcore.common.{ConfigAndPacts, Helpers, PactBrokerAddressValidation}
import com.itv.scalapactcore.{Pact, ScalaPactWriter}

import scalaj.http.{Http, HttpResponse}
import scalaz.{-\/, \/, \/-}

case class PublishDetails(pactBrokerAddress: String, providerName: String, consumerName: String, versionToPublishAs: String, pact: Pact)

case class Publisher(httpService: HttpService, addressGenerator: PublishAddressGenerator, pactFormatter: PactFormatter) {
  def apply(publishDetails: PublishDetails) = {
    val address = addressGenerator(publishDetails)
    println(s"Publishing to: $address".yellow)
    val request = Http(address).header("Content-Type", "application/json").postData(pactFormatter(publishDetails.pact)).method("PUT")
    httpService(request) match {
      case r: HttpResponse[String] if r.is2xx => println("Success".green)
      case r: HttpResponse[String] =>
        println(r)
        println(s"Failed: ${r.code}, ${r.body}".red)
    }
  }
}


object Publisher {
  val httpService: HttpService = httpRequest => httpRequest.asString

  val pactFormatter: PactFormatter = pact => ScalaPactWriter.pactToJsonString(pact)

  val addressGenerator: PublishAddressGenerator = { publishDetails =>
    import publishDetails._
    pactBrokerAddress + "/pacts/provider/" + providerName + "/consumer/" + consumerName + "/version/" + versionToPublishAs.replace("-SNAPSHOT", ".x")
  }

  def apply() = new Publisher(httpService, addressGenerator, pactFormatter)

  def pactToDetails(pactBrokerAddress: String, versionToPublishAs: String) (pact: Pact): \/[String, PublishDetails] = {
    for {
      validatedAddress <- PactBrokerAddressValidation.checkPactBrokerAddress(pactBrokerAddress)
      providerName <- Helpers.urlEncode(pact.provider.name)
      consumerName <- Helpers.urlEncode(pact.consumer.name)
    } yield PublishDetails(validatedAddress, providerName, consumerName, versionToPublishAs, pact)

  }

  def publishToBroker: Publisher => String => String => ConfigAndPacts => Unit = publisher => pactBrokerAddress => versionToPublishAs => configAndPacts => {
    configAndPacts.pacts.foreach { pact =>
      pactToDetails(pactBrokerAddress, versionToPublishAs)(pact) match {
        case -\/(l) => println(l.red)
        case \/-(details) => publisher(details)
      }
    }
    Unit
  }

}

package com.itv.scalapact.plugin.publish

import java.net.URLEncoder
import java.nio.charset.StandardCharsets

import com.itv.scalapact.plugin.common.{ConfigAndPacts, PactBrokerAddressValidation}
import com.itv.scalapact.plugin.common.Rainbow._
import com.itv.scalapactcore.ScalaPactWriter

import scalaj.http.{Http, HttpResponse}
import scalaz.{-\/, \/-}

object Publisher {

  lazy val publishToBroker: String => String => ConfigAndPacts => Unit = pactBrokerAddress => projectVersion => configAndPacts => {

    PactBrokerAddressValidation.checkPactBrokerAddress(pactBrokerAddress) match {
      case -\/(l) =>
        println(l.red)

      case \/-(r) =>
        configAndPacts.pacts.foreach { pact =>

          //TODO: Snapshot version number handling? Find out if it works or not.

          val address = r + "/pacts/provider/" + urlEncode(pact.provider.name) + "/consumer/" + urlEncode(pact.consumer.name) + "/version/" + projectVersion

          println(s"Publishing to: $address".yellow)

          Http(address).method("PUT").postData(ScalaPactWriter.pactToJsonString(pact)).asString match {
            case r: HttpResponse[String] if r.is2xx => println("Success".green)
            case r: HttpResponse[String] => println(s"Failed: ${r.body}".red)
          }

        }
    }

    Unit
  }

  val urlEncode: String => String = str => URLEncoder.encode(str, StandardCharsets.UTF_8.toString)
}

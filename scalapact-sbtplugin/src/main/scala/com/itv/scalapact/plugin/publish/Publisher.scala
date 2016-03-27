package com.itv.scalapact.plugin.publish

import java.net.URLEncoder
import java.nio.charset.StandardCharsets

import com.itv.scalapact.plugin.common.ConfigAndPacts
import com.itv.scalapact.plugin.common.Rainbow._
import com.itv.scalapactcore.ScalaPactWriter

import scalaj.http.{Http, HttpResponse}

object Publisher {

  lazy val publishToBroker: String => String => ConfigAndPacts => Unit = pactBrokerAddress => projectVersion => configAndPacts => {

    val urlMatch = "^(http)(s?)(://)([A-Za-z0-9-\\./]+)([A-Za-z0-9]$)"

    pactBrokerAddress match {
      case a: String if a.isEmpty =>
        println("Pact broker address not set, you need to add the following line to you SBT file: ".red)
        println("pactBrokerAddress := \"http://pactbroker.myserver.com\"".red)

      case a: String if !a.startsWith("http") =>
        println("Pact broker address does not appear to be valid, should start with 'http(s)' protocol.".red)

      case a: String if !a.matches(urlMatch) =>
        println("Pact broker address does not appear to be valid, should be of form: http://my.broker-address.com".red)

      case _ =>
        configAndPacts.pacts.foreach { pact =>

          //TODO: Snapshot version number handling? Find out if it works or not.

          val address = pactBrokerAddress + "/pacts/provider/" + urlEncode(pact.provider.name) + "/consumer/" + urlEncode(pact.consumer.name) + "/version/" + projectVersion

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

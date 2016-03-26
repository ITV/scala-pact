package com.itv.scalapact.plugin.publish

import java.net.URLEncoder
import java.nio.charset.StandardCharsets

import com.itv.scalapact.plugin.common.ConfigAndPacts
import com.itv.scalapact.plugin.common.Rainbow._
import com.itv.scalapactcore.ScalaPactWriter

import scalaj.http.{Http, HttpResponse}

object Publisher {

  lazy val publishToBroker: String => String => ConfigAndPacts => Unit = pactBrokerAddress => projectVersion => configAndPacts => {

    val brokerAddress =
      (configAndPacts.arguments.host, configAndPacts.arguments.port) match {
        case (Some(h), None) if h.startsWith("http://") => h
        case (Some(h), None) if h.contains(":") => "http://" + h
        case (Some(h), Some(p)) => "http://" + h + ":" + p
        case (None, None) => pactBrokerAddress
      }

    configAndPacts.pacts.foreach { pact =>

      //TODO: Snapshot version number handling?

      val address = brokerAddress + "/pacts/provider/" + urlEncode(pact.provider.name) + "/consumer/" + urlEncode(pact.consumer.name) + "/version/" + projectVersion

      println(s"Publishing to: $address".yellow)

      Http(address).method("PUT").postData(ScalaPactWriter.pactToJsonString(pact)).asString match {
        case r: HttpResponse[String] if r.is2xx => println("Success".green)
        case r: HttpResponse[String] => println(s"Failed: ${r.body}".red)
      }

    }

    Unit
  }

  val urlEncode: String => String = str => URLEncoder.encode(str, StandardCharsets.UTF_8.toString)
}

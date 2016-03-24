package com.itv.scalapact.plugin.publish

import java.net.URLEncoder
import java.nio.charset.StandardCharsets

import com.itv.scalapact.plugin.common.ConfigAndPacts

object Publisher {

  lazy val publishToBroker: String => String => ConfigAndPacts => Unit = pactBrokerAddress => projectVersion => configAndPacts => {

    configAndPacts.pacts.foreach { pact =>

      //TODO: Fallbacks for host / port i.e. commandline -> sbt config -> ???
      //TODO: Snapshot version number handling?
      //TODO: Publish using PUT

      val address = pactBrokerAddress + "/pacts/provider/" + urlEncode(pact.provider.name) + "/consumer/" + urlEncode(pact.consumer.name) + "/version/" + projectVersion

      println("Publish to: " + address)

    }

    Unit
  }

  val urlEncode: String => String = str => URLEncoder.encode(str, StandardCharsets.UTF_8.toString)
}

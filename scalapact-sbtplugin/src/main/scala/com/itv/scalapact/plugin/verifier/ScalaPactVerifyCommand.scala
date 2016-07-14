package com.itv.scalapact.plugin.verifier

import com.itv.scalapact.plugin.ScalaPactPlugin
import com.itv.scalapactcore.common.CommandArguments._
import com.itv.scalapactcore.common.ColourOuput._
import sbt._

import scala.language.implicitConversions

import Verifier._

object ScalaPactVerifyCommand {

  lazy val pactVerifyCommandHyphen: Command = Command.args("pact-verify", "<options>")(pactVerify)
  lazy val pactVerifyCommandCamel: Command = Command.args("pactVerify", "<options>")(pactVerify)

  implicit def pStateConversion(ps: Seq[(String, String => Boolean)]): List[ProviderState] =
    ps.toList.map(p => ProviderState(p._1, p._2))

  private lazy val pactVerify: (State, Seq[String]) => State = (state, args) => {

    println("*************************************".white.bold)
    println("** ScalaPact: Running Verifier     **".white.bold)
    println("*************************************".white.bold)

    val pactVerifySettings = PactVerifySettings(
      providerStates = Project.extract(state).get(ScalaPactPlugin.providerStates),
      pactBrokerAddress = Project.extract(state).get(ScalaPactPlugin.pactBrokerAddress),
      projectVersion = Project.extract(state).get(Keys.version),
      providerName = Project.extract(state).get(ScalaPactPlugin.providerName),
      consumerNames = Project.extract(state).get(ScalaPactPlugin.consumerNames).toList,
      versionedConsumerNames =
        Project.extract(state).get(ScalaPactPlugin.versionedConsumerNames).toList
        .map(t => VersionedConsumer(t._1, t._2))
    )

    val successfullyVerified = (parseArguments andThen verify(pactVerifySettings)) (args)

    if(successfullyVerified) sys.exit(0) else sys.exit(1)

    state
  }
}

case class ProviderState(key: String, f: String => Boolean)
case class VersionedConsumer(name: String, version: String)
case class PactVerifySettings(providerStates: List[ProviderState], pactBrokerAddress: String, projectVersion: String, providerName: String, consumerNames: List[String], versionedConsumerNames: List[VersionedConsumer])

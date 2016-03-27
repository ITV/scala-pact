package com.itv.scalapact.plugin.verifier

import com.itv.scalapact.plugin.ScalaPactPlugin
import com.itv.scalapact.plugin.common.CommandArguments._
import com.itv.scalapact.plugin.common.Rainbow._
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

    val providerStates: List[ProviderState] = Project.extract(state).get(ScalaPactPlugin.providerStates)
    val pactBrokerAddress: String = Project.extract(state).get(ScalaPactPlugin.pactBrokerAddress)
    val projectVersion: String = Project.extract(state).get(Keys.version)

    (parseArguments andThen verify(providerStates)(projectVersion)(pactBrokerAddress)) (args)

    state
  }
}

case class ProviderState(key: String, f: String => Boolean)

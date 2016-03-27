package com.itv.scalapact.plugin.publish

import com.itv.scalapact.plugin.ScalaPactPlugin
import com.itv.scalapact.plugin.common.CommandArguments
import com.itv.scalapact.plugin.common.LocalPactFileLoader
import com.itv.scalapact.plugin.common.Rainbow._
import sbt._

object ScalaPactPublishCommand {

  lazy val pactPublishCommandHyphen: Command = Command.args("pact-publish", "<options>")(pactVerify)
  lazy val pactPublishCommandCamel: Command = Command.args("pactPublish", "<options>")(pactVerify)

  private lazy val pactVerify: (State, Seq[String]) => State = (state, args) => {

    import CommandArguments._
    import LocalPactFileLoader._
    import Publisher._

    println("*************************************".white.bold)
    println("** ScalaPact: Publishing Contracts **".white.bold)
    println("*************************************".white.bold)

    val pactTestedState = Command.process("pact-test", state)

    val pactBrokerAddress: String = Project.extract(pactTestedState).get(ScalaPactPlugin.pactBrokerAddress)
    val projectVersion: String = Project.extract(pactTestedState).get(Keys.version)

    (parseArguments andThen loadPactFiles("target/pacts") andThen publishToBroker(pactBrokerAddress)(projectVersion)) (args)

    pactTestedState
  }
}
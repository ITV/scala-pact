package com.itv.scalapact.plugin.publish

import com.itv.scalapact.plugin.ScalaPactPlugin
import com.itv.scalapactcore.common.CommandArguments._
import com.itv.scalapactcore.common.LocalPactFileLoader._
import com.itv.scalapactcore.common.ColourOuput._
import sbt._

object ScalaPactPublishCommand {

  lazy val pactPublishCommandHyphen: Command = Command.args("pact-publish", "<options>")(pactVerify)
  lazy val pactPublishCommandCamel: Command = Command.args("pactPublish", "<options>")(pactVerify)

  private lazy val pactVerify: (State, Seq[String]) => State = (state, args) => {

    import Publisher._

    println("*************************************".white.bold)
    println("** ScalaPact: Publishing Contracts **".white.bold)
    println("*************************************".white.bold)

    val pactTestedState = Command.process("pact-test", state)

    val pactBrokerAddress: String = Project.extract(pactTestedState).get(ScalaPactPlugin.pactBrokerAddress)
    val projectVersion: String = Project.extract(pactTestedState).get(Keys.version)
    val pactContractVersion: String = Project.extract(pactTestedState).get(ScalaPactPlugin.pactContractVersion)

    val versionToPublishAs = if(pactContractVersion.isEmpty) projectVersion else pactContractVersion

    (parseArguments andThen loadPactFiles("target/pacts") andThen publishToBroker(pactBrokerAddress)(versionToPublishAs)) (args)

    pactTestedState
  }
}
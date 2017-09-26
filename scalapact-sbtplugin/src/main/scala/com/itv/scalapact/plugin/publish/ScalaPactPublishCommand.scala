package com.itv.scalapact.plugin.publish

import com.itv.scalapact.plugin.ScalaPactPlugin
import com.itv.scalapactcore.common.CommandArguments._
import com.itv.scalapact.shared.ColourOuput._
import com.itv.scalapactcore.common.LocalPactFileLoader
import sbt._
import com.itv.scalapactcore.common.PactReaderWriter._

object ScalaPactPublishCommand {

  lazy val pactPublishCommandHyphen: Command = Command.args("pact-publish", "<options>")(pactVerify)
  lazy val pactPublishCommandCamel: Command = Command.args("pactPublish", "<options>")(pactVerify)

  private lazy val pactVerify: (State, Seq[String]) => State = (state, args) => {
    val pactTestedState = Command.process("pact-test", state)

    doPactPublish(args, Option(state))

    pactTestedState
  }

  def doPactPublish(args: Seq[String], extractedState: Option[State]): Unit = {
    import Publisher._

    println("*************************************".white.bold)
    println("** ScalaPact: Publishing Contracts **".white.bold)
    println("*************************************".white.bold)

    val pactBrokerAddress: String =
      extractedState.map { pactTestedState =>
        Project.extract(pactTestedState).get(ScalaPactPlugin.autoImport.pactBrokerAddress)
      }.getOrElse(ScalaPactPlugin.autoImport.pactBrokerAddress.value)
    val projectVersion: String =
      extractedState.map { pactTestedState =>
        Project.extract(pactTestedState).get(Keys.version)
      }.getOrElse(Keys.version.value)
    val pactContractVersion: String =
      extractedState.map { pactTestedState =>
        Project.extract(pactTestedState).get(ScalaPactPlugin.autoImport.pactContractVersion)
      }.getOrElse(ScalaPactPlugin.autoImport.pactContractVersion.value)
    val allowSnapshotPublish: Boolean =
      extractedState.map { pactTestedState =>
        Project.extract(pactTestedState).get(ScalaPactPlugin.autoImport.allowSnapshotPublish)
      }.getOrElse(ScalaPactPlugin.autoImport.allowSnapshotPublish.value)

    val versionToPublishAs = if(pactContractVersion.isEmpty) projectVersion else pactContractVersion

    val loadPactFiles = LocalPactFileLoader.loadPactFiles

    if(versionToPublishAs.contains("SNAPSHOT") && !allowSnapshotPublish) {
      println("Snapshot pact file publishing not permitted".red.bold)
      println("Publishing of pact contracts against snapshot versions is not allowed by default.".yellow)
      println("Pact broker does not cope well with snapshot contracts.".yellow)
      println("To enable this feature, add \"allowSnapshotPublish := true\" to your pact.sbt file.".yellow)
    } else {
      (parseArguments andThen loadPactFiles("target/pacts") andThen publishToBroker(pactBrokerAddress, versionToPublishAs)) (args)
    }
  }
}
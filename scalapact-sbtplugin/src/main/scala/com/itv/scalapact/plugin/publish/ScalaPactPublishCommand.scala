package com.itv.scalapact.plugin.publish

import com.itv.scalapact.plugin.ScalaPactPlugin
import com.itv.scalapact.shared.ColourOuput._
import com.itv.scalapact.shared.ScalaPactSettings
import com.itv.scalapactcore.common.LocalPactFileLoader
import sbt._
import com.itv.scalapactcore.common.PactReaderWriter._

object ScalaPactPublishCommand {

  lazy val pactPublishCommandHyphen: Command = Command.args("pact-publish", "<options>")(pactVerify)
  lazy val pactPublishCommandCamel: Command = Command.args("pactPublish", "<options>")(pactVerify)

  private lazy val pactVerify: (State, Seq[String]) => State = (state, args) => {
    val pactTestedState = Command.process("pact-test", state)

    doPactPublish(
      ScalaPactSettings.parseArguments(args),
      Project.extract(pactTestedState).get(ScalaPactPlugin.autoImport.pactBrokerAddress),
      Project.extract(pactTestedState).get(Keys.version),
      Project.extract(pactTestedState).get(ScalaPactPlugin.autoImport.pactContractVersion),
      Project.extract(pactTestedState).get(ScalaPactPlugin.autoImport.allowSnapshotPublish)
    )

    pactTestedState
  }

  def doPactPublish(scalaPactSettings: ScalaPactSettings, pactBrokerAddress: String, projectVersion: String, pactContractVersion: String, allowSnapshotPublish: Boolean): Unit = {
    import Publisher._

    println("*************************************".white.bold)
    println("** ScalaPact: Publishing Contracts **".white.bold)
    println("*************************************".white.bold)

    val versionToPublishAs = if(pactContractVersion.isEmpty) projectVersion else pactContractVersion

    val loadPactFiles = LocalPactFileLoader.loadPactFiles

    if(versionToPublishAs.contains("SNAPSHOT") && !allowSnapshotPublish) {
      println("Snapshot pact file publishing not permitted".red.bold)
      println("Publishing of pact contracts against snapshot versions is not allowed by default.".yellow)
      println("Pact broker does not cope well with snapshot contracts.".yellow)
      println("To enable this feature, add \"allowSnapshotPublish := true\" to your pact.sbt file.".yellow)
    } else {
      (loadPactFiles("target/pacts") andThen publishToBroker(pactBrokerAddress, versionToPublishAs)) (scalaPactSettings)
    }
  }
}
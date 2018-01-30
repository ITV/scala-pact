package com.itv.scalapact.plugin.publish

import com.itv.scalapact.plugin.ScalaPactPlugin
import com.itv.scalapact.shared.ColourOuput._
import com.itv.scalapact.shared.http.ScalaPactHttpClient
import com.itv.scalapact.shared.{ConfigAndPacts, ScalaPactSettings}
import com.itv.scalapactcore.common.LocalPactFileLoader
import sbt._
import com.itv.scalapactcore.common.PactReaderWriter._
import com.itv.scalapact.shared.PactLogger

object ScalaPactPublishCommand {

  lazy val pactPublishCommandHyphen: Command = Command.args("pact-publish", "<options>")(pactVerify)
  lazy val pactPublishCommandCamel: Command = Command.args("pactPublish", "<options>")(pactVerify)

  private lazy val pactVerify: (State, Seq[String]) => State = (state, args) => {
    val pactTestedState = Command.process("pact-test", state)

    doPactPublish(
      Project.extract(state).get(ScalaPactPlugin.autoImport.scalaPactEnv).toSettings + ScalaPactSettings.parseArguments(args),
      Project.extract(pactTestedState).get(ScalaPactPlugin.autoImport.pactBrokerAddress),
      Project.extract(pactTestedState).get(ScalaPactPlugin.autoImport.providerBrokerPublishMap),
      Project.extract(pactTestedState).get(Keys.version),
      Project.extract(pactTestedState).get(ScalaPactPlugin.autoImport.pactContractVersion),
      Project.extract(pactTestedState).get(ScalaPactPlugin.autoImport.allowSnapshotPublish)
    )

    pactTestedState
  }

  def doPactPublish(scalaPactSettings: ScalaPactSettings, pactBrokerAddress: String, providerBrokerPublishMap: Map[String, String], projectVersion: String, pactContractVersion: String, allowSnapshotPublish: Boolean): Unit = {
    import Publisher._

    PactLogger.message("*************************************".white.bold)
    PactLogger.message("** ScalaPact: Publishing Contracts **".white.bold)
    PactLogger.message("*************************************".white.bold)

    val versionToPublishAs = if(pactContractVersion.isEmpty) projectVersion else pactContractVersion

    if(versionToPublishAs.contains("SNAPSHOT") && !allowSnapshotPublish) {
      PactLogger.error("Snapshot pact file publishing not permitted".red.bold)
      PactLogger.error("Publishing of pact contracts against snapshot versions is not allowed by default.".yellow)
      PactLogger.error("Pact broker does not cope well with snapshot contracts.".yellow)
      PactLogger.error("To enable this feature, add \"allowSnapshotPublish := true\" to your pact.sbt file.".yellow)
    } else {
      val configAndPactFiles = LocalPactFileLoader.loadPactFiles(pactReader)(false)(scalaPactSettings.giveOutputPath)(scalaPactSettings)

      // Publish all to main broker
      publishToBroker(ScalaPactHttpClient.doRequestSync, pactBrokerAddress, versionToPublishAs)(pactWriter)(configAndPactFiles).foreach(r => PactLogger.message(r.renderAsString))

      // Publish to other specified brokers
      configAndPactFiles.pacts.foreach { pactContract =>
        providerBrokerPublishMap.get(pactContract.provider.name).foreach { broker =>
          publishToBroker(ScalaPactHttpClient.doRequestSync, broker, versionToPublishAs)(pactWriter)(ConfigAndPacts(scalaPactSettings, List(pactContract))).foreach(r => PactLogger.message(r.renderAsString))
        }
      }

    }
  }
}
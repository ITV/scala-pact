package com.itv.scalapact.plugin

import com.itv.scalapact.plugin.publish.ScalaPactPublishCommand
import com.itv.scalapact.plugin.stubber.ScalaPactStubberCommand
import com.itv.scalapact.plugin.tester.ScalaPactTestCommand
import com.itv.scalapact.plugin.verifier.ScalaPactVerifyCommand
import sbt.Keys._
import sbt._

import scala.language.implicitConversions

object ScalaPactPlugin extends AutoPlugin {
  override def requires = plugins.JvmPlugin
  override def trigger = allRequirements

  object autoImport {
    val providerStateMatcher = SettingKey[PartialFunction[String, Boolean]]("provider-state-matcher", "Alternative partial function for provider state setup")
    val providerStates = SettingKey[Seq[(String, String => Boolean)]]("provider-states", "A list of provider state setup functions")
    val pactBrokerAddress = SettingKey[String]("pactBrokerAddress", "The base url to publish / pull pact contract files to and from.")
    val providerName = SettingKey[String]("providerName", "The name of the service to verify")
    val consumerNames = SettingKey[Seq[String]]("consumerNames", "The names of the services that consume the service to verify")
    val versionedConsumerNames = SettingKey[Seq[(String, String)]]("versionedConsumerNames", "The name and pact version numbers of the services that consume the service to verify")
    val pactContractVersion = SettingKey[String]("pactContractVersion", "The version number the pact contract will be published under. If missing or empty, the project version will be used.")
    val allowSnapshotPublish = SettingKey[Boolean]("allowSnapshotPublish", "Flag to permit publishing of snapshot pact files to pact broker. Default is false.")

    // Tasks
    val pactPack = taskKey[Unit]("Pack up Pact contract files")
    val pactPush = taskKey[Unit]("Push Pact contract files to Pact Broker")
    val pactCheck = taskKey[Unit]("Verify service based on consumer requirements")
    val pactStub = taskKey[Unit]("Run stub service from Pact contract files")
  }

  import autoImport._

  private val pactSettings = Seq(
    providerStateMatcher := PartialFunction { (_: String) => false },
    providerStates := Seq(),
    pactBrokerAddress := "",
    providerName := "",
    consumerNames := Seq.empty[String],
    versionedConsumerNames := Seq.empty[(String,String)],
    pactContractVersion := "",
    allowSnapshotPublish := false
  )

  override lazy val projectSettings = Seq(
    commands += ScalaPactTestCommand.pactTestCommandHyphen,
    commands += ScalaPactTestCommand.pactTestCommandCamel,

    commands += ScalaPactPublishCommand.pactPublishCommandHyphen,
    commands += ScalaPactPublishCommand.pactPublishCommandCamel,

    commands += ScalaPactVerifyCommand.pactVerifyCommandHyphen,
    commands += ScalaPactVerifyCommand.pactVerifyCommandCamel,

    commands += ScalaPactStubberCommand.pactStubberCommandHyphen,
    commands += ScalaPactStubberCommand.pactStubberCommandCamel
  ) ++ pactSettings

  override lazy val buildSettings = Seq(
    pactPack := pactPackTask.value,
    pactPush := pactPushTask.value,
    pactCheck := pactCheckTask.value,
    pactStub := pactStubTask.value
  )

  lazy val pactPackTask =
    Def.task {
      ScalaPactTestCommand.doPactPack()
    }

  lazy val pactPushTask =
    Def.task {
      ScalaPactPublishCommand.doPactPublish(Seq(), None)
    }

  lazy val pactCheckTask =
    Def.task {
      ScalaPactVerifyCommand.doPactVerify(Seq(), None)
    }

  lazy val pactStubTask =
    Def.task {
      ScalaPactTestCommand.doPactPack()
    }
}

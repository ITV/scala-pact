package com.itv.scalapact.plugin

import com.itv.scalapact.plugin.publish.ScalaPactPublishCommand
import com.itv.scalapact.plugin.stubber.ScalaPactStubberCommand
import com.itv.scalapact.plugin.tester.ScalaPactTestCommand
import com.itv.scalapact.plugin.verifier.ScalaPactVerifyCommand
import com.itv.scalapact.shared.ScalaPactSettings
import sbt.Keys._
import sbt.plugins.JvmPlugin
import sbt.{Def, _}

import scala.language.implicitConversions

object ScalaPactPlugin extends AutoPlugin {
  override def requires: JvmPlugin.type = plugins.JvmPlugin
  override def trigger: PluginTrigger = allRequirements

  object autoImport {
    val providerStateMatcher: SettingKey[PartialFunction[String, Boolean]] =
      SettingKey[PartialFunction[String, Boolean]]("provider-state-matcher", "Alternative partial function for provider state setup")

    val providerStates: SettingKey[Seq[(String, (String) => Boolean)]] =
      SettingKey[Seq[(String, String => Boolean)]]("provider-states", "A list of provider state setup functions")

    val pactBrokerAddress: SettingKey[String] =
      SettingKey[String]("pactBrokerAddress", "The base url to publish / pull pact contract files to and from.")

    val providerName: SettingKey[String] =
      SettingKey[String]("providerName", "The name of the service to verify")

    val consumerNames: SettingKey[Seq[String]] =
      SettingKey[Seq[String]]("consumerNames", "The names of the services that consume the service to verify")

    val versionedConsumerNames: SettingKey[Seq[(String, String)]] =
      SettingKey[Seq[(String, String)]]("versionedConsumerNames", "The name and pact version numbers of the services that consume the service to verify")

    val pactContractVersion: SettingKey[String] =
      SettingKey[String]("pactContractVersion", "The version number the pact contract will be published under. If missing or empty, the project version will be used.")

    val allowSnapshotPublish: SettingKey[Boolean] =
      SettingKey[Boolean]("allowSnapshotPublish", "Flag to permit publishing of snapshot pact files to pact broker. Default is false.")

    val scalaPactSettings: SettingKey[ScalaPactSettings] =
      SettingKey[ScalaPactSettings]("scalaPactSettings", "Settings used to config the running of tasks and commands")

    // Tasks
    val pactPack: TaskKey[Unit] = taskKey[Unit]("Pack up Pact contract files")
    val pactPush: TaskKey[Unit] = taskKey[Unit]("Push Pact contract files to Pact Broker")
    val pactCheck: TaskKey[Unit] = taskKey[Unit]("Verify service based on consumer requirements")
    val pactStub: TaskKey[Unit] = taskKey[Unit]("Run stub service from Pact contract files")
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
    allowSnapshotPublish := false,
    scalaPactSettings := ScalaPactSettings.default
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

  lazy val pactPackTask: Def.Initialize[Task[Unit]] =
    Def.task {
      ScalaPactTestCommand.doPactPack()
    }

  lazy val pactPushTask: Def.Initialize[Task[Unit]] =
    Def.task {
      ScalaPactPublishCommand.doPactPublish(
        scalaPactSettings.value,
        pactBrokerAddress.value,
        version.value,
        pactContractVersion.value,
        allowSnapshotPublish.value
      )
    }

  lazy val pactCheckTask: Def.Initialize[Task[Unit]] =
    Def.task {
      ScalaPactVerifyCommand.doPactVerify(
        scalaPactSettings.value,
        providerStates.value,
        providerStateMatcher.value,
        pactBrokerAddress.value,
        version.value,
        providerName.value,
        consumerNames.value,
        versionedConsumerNames.value
      )
    }

  lazy val pactStubTask: Def.Initialize[Task[Unit]] =
    Def.task {
      ScalaPactStubberCommand.runStubber(
        scalaPactSettings.value,
        ScalaPactStubberCommand.interactionManagerInstance
      )
    }
}

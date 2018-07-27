package com.itv.scalapact.plugin

import com.itv.scalapact.http._
import com.itv.scalapact.json._
import com.itv.scalapact.plugin.shared._
import com.itv.scalapact.shared.ScalaPactSettings
import com.itv.scalapactcore.verifier.Verifier.{ProviderStateResult, SetupProviderState}
import sbt.Keys._
import sbt.plugins.JvmPlugin
import sbt.{Def, _}
import complete.DefaultParsers._

object ScalaPactPlugin extends AutoPlugin {
  override def requires: JvmPlugin.type = plugins.JvmPlugin
  override def trigger: PluginTrigger   = allRequirements

  object autoImport {
    implicit def toSetupProviderState(bool: Boolean): ProviderStateResult = ProviderStateResult(bool)

    val providerStateMatcher: SettingKey[PartialFunction[String, ProviderStateResult]] =
      SettingKey[PartialFunction[String, ProviderStateResult]]("provider-state-matcher", "Alternative partial function for provider state setup")

    val providerStates: SettingKey[Seq[(String, SetupProviderState)]] =
      SettingKey[Seq[(String, SetupProviderState)]]("provider-states", "A list of provider state setup functions")

    val pactBrokerAddress: SettingKey[String] =
      SettingKey[String]("pactBrokerAddress", "The base url to publish / pull pact contract files to and from.")

    val providerBrokerPublishMap: SettingKey[Map[String, String]] =
      SettingKey[Map[String, String]](
        "providerBrokerPublishMap",
        "An optional map of this consumer's providers, and alternate pact brokers to publish those contracts to."
      )

    val providerName: SettingKey[String] =
      SettingKey[String]("providerName", "The name of the service to verify")

    val consumerNames: SettingKey[Seq[String]] =
      SettingKey[Seq[String]]("consumerNames", "The names of the services that consume the service to verify")

    val versionedConsumerNames: SettingKey[Seq[(String, String)]] =
      SettingKey[Seq[(String, String)]](
        "versionedConsumerNames",
        "The name and pact version numbers of the services that consume the service to verify"
      )

    val pactContractVersion: SettingKey[String] =
      SettingKey[String](
        "pactContractVersion",
        "The version number the pact contract will be published under. If missing or empty, the project version will be used."
      )

    val allowSnapshotPublish: SettingKey[Boolean] =
      SettingKey[Boolean]("allowSnapshotPublish",
                          "Flag to permit publishing of snapshot pact files to pact broker. Default is false.")

    val scalaPactEnv: SettingKey[ScalaPactEnv] =
      SettingKey[ScalaPactEnv]("scalaPactEnv", "Settings used to config the running of tasks and commands")

    // Tasks
    val pactPack: TaskKey[Unit]   = taskKey[Unit]("Pack up Pact contract files")
    val pactPush: InputKey[Unit]  = inputKey[Unit]("Push Pact contract files to Pact Broker")
    val pactCheck: InputKey[Unit] = inputKey[Unit]("Verify service based on consumer requirements")
    val pactStub: InputKey[Unit]  = inputKey[Unit]("Run stub service from Pact contract files")

    val pactTest: TaskKey[Unit]     = taskKey[Unit]("clean, compile, test and then pactPack")
    val pactPublish: InputKey[Unit] = inputKey[Unit]("pactTest and then pactPush")
    val pactVerify: InputKey[Unit]  = inputKey[Unit]("pactCheck")
    val pactStubber: InputKey[Unit] = inputKey[Unit]("pactTest and then pactStub")

  }

  import autoImport._

  private val pf: PartialFunction[String, ProviderStateResult] = { case (_: String) => false }

  @SuppressWarnings(Array("org.wartremover.warts.Any"))
  private val pactSettings = Seq(
    providerStateMatcher := pf,
    providerStates := Seq(),
    pactBrokerAddress := "",
    providerBrokerPublishMap := Map.empty[String, String],
    providerName := "",
    consumerNames := Seq.empty[String],
    versionedConsumerNames := Seq.empty[(String, String)],
    pactContractVersion := "",
    allowSnapshotPublish := false,
    scalaPactEnv := ScalaPactEnv.empty
  )

  @SuppressWarnings(Array("org.wartremover.warts.Any"))
  override lazy val projectSettings: Seq[Def.Setting[
    _ >: Seq[(String, SetupProviderState)] with Seq[(String, String)] with Boolean with ScalaPactEnv with Map[
      String,
      String
    ] with String with Seq[String] with PartialFunction[String, ProviderStateResult] with Task[Unit] with InputTask[Unit]
  ]] = {
    pactSettings ++ Seq(
      // Tasks
      pactPack := pactPackTask.value,
      pactPush := pactPushTask.evaluated,
      pactCheck := pactCheckTask.evaluated,
      pactStub := pactStubTask.evaluated,
      // Classic
      pactTest := pactTestTask.value,
      pactPublish := pactPublishTask.evaluated,
      pactVerify := pactCheckTask.evaluated,
      pactStubber := pactStubberTask.evaluated
    )
  }

  @SuppressWarnings(Array("org.wartremover.warts.Any"))
  lazy val pactPackTask: Def.Initialize[Task[Unit]] =
    Def.task {
      ScalaPactTestCommand.doPactPack(scalaPactEnv.value.toSettings)
    }

  @SuppressWarnings(Array("org.wartremover.warts.Any"))
  def pactPushTask: Def.Initialize[InputTask[Unit]] =
    Def.inputTask {
      ScalaPactPublishCommand.doPactPublish(
        scalaPactEnv.value.toSettings + ScalaPactSettings.parseArguments(spaceDelimited("<arg>").parsed),
        pactBrokerAddress.value,
        providerBrokerPublishMap.value,
        version.value,
        pactContractVersion.value,
        allowSnapshotPublish.value
      )
    }

  @SuppressWarnings(Array("org.wartremover.warts.Any"))
  def pactCheckTask: Def.Initialize[InputTask[Unit]] =
    Def.inputTask {
      ScalaPactVerifyCommand.doPactVerify(
        scalaPactEnv.value.toSettings + ScalaPactSettings.parseArguments(spaceDelimited("<arg>").parsed),
        providerStates.value,
        providerStateMatcher.value,
        pactBrokerAddress.value,
        version.value,
        providerName.value,
        consumerNames.value,
        versionedConsumerNames.value
      )
    }

  @SuppressWarnings(Array("org.wartremover.warts.Any"))
  def pactStubTask: Def.Initialize[InputTask[Unit]] =
    Def.inputTask {
      ScalaPactStubberCommand
        .runStubber(
          scalaPactEnv.value.toSettings + ScalaPactSettings.parseArguments(spaceDelimited("<arg>").parsed),
          ScalaPactStubberCommand.interactionManagerInstance
        )
    }

  @SuppressWarnings(Array("org.wartremover.warts.Any"))
  def pactTestTask: Def.Initialize[Task[Unit]] =
    pactPackTask.dependsOn((test in Test).dependsOn((compile in Compile).dependsOn(clean in Compile)))

  @SuppressWarnings(Array("org.wartremover.warts.Any"))
  def pactPublishTask: Def.Initialize[InputTask[Unit]] = Def.inputTask {
    pactPushTask.dependsOn(pactTestTask).evaluated
  }

  @SuppressWarnings(Array("org.wartremover.warts.Any"))
  def pactStubberTask: Def.Initialize[InputTask[Unit]] = Def.inputTask {
    pactStubTask.dependsOn(pactTestTask).evaluated
  }

}

package com.itv.scalapact.plugin

import com.itv.scalapact.http._
import com.itv.scalapact.json._
import com.itv.scalapact.plugin.shared._
import com.itv.scalapact.shared._
import com.itv.scalapact.shared.ProviderStateResult.SetupProviderState
import sbt.Keys._
import sbt.plugins.JvmPlugin
import sbt.{Def, _}
import complete.DefaultParsers._

import scala.concurrent.duration._
import scala.language.implicitConversions

object ScalaPactPlugin extends AutoPlugin {
  override def requires: JvmPlugin.type = plugins.JvmPlugin
  override def trigger: PluginTrigger   = noTrigger

  @SuppressWarnings(Array("org.wartremover.warts.ImplicitConversion"))
  implicit def booleanToProviderStateResult(bool: Boolean): ProviderStateResult = ProviderStateResult(bool)

  object autoImport {
    val providerStateMatcher: SettingKey[PartialFunction[String, ProviderStateResult]] =
      SettingKey[PartialFunction[String, ProviderStateResult]](
        "provider-state-matcher",
        "Alternative partial function for provider state setup"
      )

    val providerStates: SettingKey[Seq[(String, SetupProviderState)]] =
      SettingKey[Seq[(String, SetupProviderState)]]("provider-states", "A list of provider state setup functions")

    val pactBrokerAddress: SettingKey[String] =
      SettingKey[String]("pactBrokerAddress", "The base url to publish / pull pact contract files to and from.")

    val pactBrokerCredentials: SettingKey[(String, String)] =
      SettingKey[(String, String)](
        "pactBrokerCredentials",
        "The basic authentication credentials (username, password) for accessing the broker."
      )

    val pactBrokerToken: SettingKey[String] =
      SettingKey[String](
        "pactBrokerToken",
        "The token used in the \"Bearer theToken\" header for accessing the broker."
      )

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

    val taggedConsumerNames: SettingKey[Seq[(String, Seq[String])]] =
      SettingKey[Seq[(String, Seq[String])]](
        "taggedConsumerNames",
        "The name and list of tags of the services that consume the service to verify"
      )

    val consumerVersionSelectors: SettingKey[Seq[ConsumerVersionSelector]] =
      SettingKey[Seq[ConsumerVersionSelector]](
        "consumerVersionSelectors",
        "the consumer version selectors to fetch pacts using the `pacts-for-verification` endpoint"
      )

    val providerVersionTags: SettingKey[Seq[String]] =
      SettingKey[Seq[String]](
        "providerVersionTags",
        "the tag name(s) for the provider application version that will be published with the verification results"
      )

    val includePendingStatus: SettingKey[Boolean] =
      SettingKey[Boolean](
        "includePendingStatus",
        ""
      )

    val pactContractVersion: SettingKey[String] =
      SettingKey[String](
        "pactContractVersion",
        "The version number the pact contract will be published under. If missing or empty, the project version will be used."
      )

    val pactContractTags: SettingKey[Seq[String]] =
      SettingKey[Seq[String]](
        "pactContractTags",
        "The tags the pact contract will be published with. If missing or empty, the contract will be published without tags."
      )

    val allowSnapshotPublish: SettingKey[Boolean] =
      SettingKey[Boolean](
        "allowSnapshotPublish",
        "Flag to permit publishing of snapshot pact files to pact broker. Default is false."
      )

    val pactBrokerClientTimeout: SettingKey[Duration] =
      SettingKey[Duration](
        "pactBrokerClientTimeout",
        "The timeout for requests when communicating with the pact broker"
      )

    val sslContextName: SettingKey[Option[String]] =
      SettingKey[Option[String]](
        "sslContextName",
        "The ssl context to extract from the defined `SslContextMap`"
      )

    val scalaPactEnv: SettingKey[ScalaPactEnv] =
      SettingKey[ScalaPactEnv]("scalaPactEnv", "Settings used to config the running of tasks and commands")

    val areScalaPactContracts: SettingKey[Boolean] = SettingKey[Boolean](
      "scalaPactContracts",
      "Whether the pacts to be published are scala-pact contracts, or pact-jvm contracts"
    )

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

  private val pf: PartialFunction[String, ProviderStateResult] = { case _: String => ProviderStateResult(false) }

  @SuppressWarnings(Array("org.wartremover.warts.Any"))
  private val pactSettings = Seq(
    providerStateMatcher := pf,
    providerStates := Seq(),
    pactBrokerAddress := "",
    providerBrokerPublishMap := Map.empty[String, String],
    providerName := "",
    consumerNames := Seq.empty[String],
    versionedConsumerNames := Seq.empty[(String, String)],
    taggedConsumerNames := Seq.empty[(String, Seq[String])],
    consumerVersionSelectors := Seq.empty[ConsumerVersionSelector],
    providerVersionTags := Seq.empty[String],
    pactContractVersion := "",
    pactContractTags := Seq.empty[String],
    allowSnapshotPublish := false,
    scalaPactEnv := ScalaPactEnv.empty,
    pactBrokerCredentials := (("", ""): (String, String)),
    pactBrokerToken := "",
    pactBrokerClientTimeout := 2.seconds,
    sslContextName := None,
    includePendingStatus := false,
    areScalaPactContracts := true
  )

  @SuppressWarnings(Array("org.wartremover.warts.Any"))
  override lazy val projectSettings: Seq[Def.Setting[_]] = {
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
      ScalaPactTestCommand.doPactPack(scalaPactEnv.value.toSettings, areScalaPactContracts.value)
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
        allowSnapshotPublish.value,
        pactContractTags.value,
        PactBrokerAuthorization(pactBrokerCredentials.value, pactBrokerToken.value),
        pactBrokerClientTimeout.value,
        sslContextName.value,
        areScalaPactContracts.value
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
        providerName.value,
        consumerNames.value,
        versionedConsumerNames.value,
        taggedConsumerNames.value,
        consumerVersionSelectors.value,
        providerVersionTags.value,
        PactBrokerAuthorization(pactBrokerCredentials.value, pactBrokerToken.value),
        pactBrokerClientTimeout.value,
        sslContextName.value,
        includePendingStatus.value
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
    pactPackTask.dependsOn((Test / test).dependsOn((Compile / compile).dependsOn(Compile / clean)))

  @SuppressWarnings(Array("org.wartremover.warts.Any"))
  def pactPublishTask: Def.Initialize[InputTask[Unit]] = Def.inputTask {
    pactPushTask.dependsOn(pactTestTask).evaluated
  }

  @SuppressWarnings(Array("org.wartremover.warts.Any"))
  def pactStubberTask: Def.Initialize[InputTask[Unit]] = Def.inputTask {
    pactStubTask.dependsOn(pactTestTask).evaluated
  }

}

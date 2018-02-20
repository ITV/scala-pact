package com.itv.scalapact.plugin

import com.itv.scalapact.plugin.publish.ScalaPactPublishCommand
import com.itv.scalapact.plugin.stubber.ScalaPactStubberCommand
import com.itv.scalapact.plugin.tester.ScalaPactTestCommand
import com.itv.scalapact.plugin.verifier.ScalaPactVerifyCommand
import com.itv.scalapact.shared.{ScalaPactSettings, SslContextMap}
import sbt.Keys._
import sbt.plugins.JvmPlugin
import sbt.{Def, _}

import scala.concurrent.duration.Duration
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

    val providerBrokerPublishMap: SettingKey[Map[String, String]] =
      SettingKey[Map[String, String]]("providerBrokerPublishMap", "An optional map of this consumer's providers, and alternate pact brokers to publish those contracts to.")

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

    val scalaPactEnv: SettingKey[ScalaPactEnv] =
      SettingKey[ScalaPactEnv]("scalaPactEnv", "Settings used to config the running of tasks and commands")

    val scalaPactSslMap: SettingKey[SslContextMap] =
      SettingKey[SslContextMap]("sslContextMap", "the context map used to allow the verifier to use ssl contexts with it's queries")

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
    providerBrokerPublishMap := Map.empty[String, String],
    providerName := "",
    consumerNames := Seq.empty[String],
    versionedConsumerNames := Seq.empty[(String,String)],
    pactContractVersion := "",
    allowSnapshotPublish := false,
    scalaPactEnv := ScalaPactEnv.default,
    scalaPactSslMap := SslContextMap.defaultEmptyContextMap
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
  ) ++ pactSettings ++ Seq(
    pactPack := pactPackTask.value,
    pactPush := pactPushTask.value,
    pactCheck := pactCheckTask.value,
    pactStub := pactStubTask.value
  )

  lazy val pactPackTask: Def.Initialize[Task[Unit]] =
    Def.task {
      ScalaPactTestCommand.doPactPack(scalaPactEnv.value.toSettings)
    }

  lazy val pactPushTask: Def.Initialize[Task[Unit]] =
    Def.task {
      ScalaPactPublishCommand.doPactPublish(
        scalaPactEnv.value.toSettings,
        pactBrokerAddress.value,
        providerBrokerPublishMap.value,
        version.value,
        pactContractVersion.value,
        allowSnapshotPublish.value
      )
    }

  lazy val pactCheckTask: Def.Initialize[Task[Unit]] =
    Def.task {
      ScalaPactVerifyCommand.doPactVerify(
        scalaPactEnv.value.toSettings,
        providerStates.value,
        providerStateMatcher.value,
        pactBrokerAddress.value,
        version.value,
        providerName.value,
        consumerNames.value,
        versionedConsumerNames.value,
        scalaPactSslMap.value
      )
    }

  lazy val pactStubTask: Def.Initialize[Task[Unit]] =
    Def.task {
      ScalaPactStubberCommand.runStubber(
        scalaPactEnv.value.toSettings,
        ScalaPactStubberCommand.interactionManagerInstance
      )
    }
}

case class ScalaPactEnv(protocol: Option[String], host: Option[String], port: Option[Int], localPactFilePath: Option[String], strictMode: Option[Boolean], clientTimeout: Option[Duration], outputPath: Option[String]) {

  def +(other: ScalaPactEnv): ScalaPactEnv =
    ScalaPactEnv.append(this, other)

  def withProtocol(protocol: String): ScalaPactEnv =
    this.copy(protocol = Option(protocol))

  def withHost(host: String): ScalaPactEnv =
    this.copy(host = Option(host))

  def withPort(port: Int): ScalaPactEnv =
    this.copy(port = Option(port))

  def withLocalPactFilePath(path: String): ScalaPactEnv =
    this.copy(localPactFilePath = Option(path))

  def enableStrictMode: ScalaPactEnv =
    this.copy(strictMode = Option(true))

  def disableStrictMode: ScalaPactEnv =
    this.copy(strictMode = Option(false))

  def withClientTimeOut(duration: Duration): ScalaPactEnv =
    this.copy(clientTimeout = Option(duration))

  def withOutputPath(outputPath: String): ScalaPactEnv =
    this.copy(outputPath = Option(outputPath))

  def toSettings: ScalaPactSettings =
    ScalaPactSettings(protocol, host, port, localPactFilePath, strictMode, clientTimeout, outputPath)

}

object ScalaPactEnv {

  def apply: ScalaPactEnv = default

  def default: ScalaPactEnv = ScalaPactEnv(None, None, None, None, None, None, None)

  def append(a: ScalaPactEnv, b: ScalaPactEnv): ScalaPactEnv =
    ScalaPactEnv(
      host = b.host.orElse(a.host),
      protocol = b.protocol.orElse(a.protocol),
      port = b.port.orElse(a.port),
      localPactFilePath = b.localPactFilePath.orElse(a.localPactFilePath),
      strictMode = b.strictMode.orElse(a.strictMode),
      clientTimeout = b.clientTimeout.orElse(a.clientTimeout),
      outputPath = b.outputPath.orElse(a.outputPath)
    )
}
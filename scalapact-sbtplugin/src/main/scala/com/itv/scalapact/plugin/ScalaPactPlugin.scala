package com.itv.scalapact.plugin

import com.itv.scalapact.plugin.publish.ScalaPactPublishCommand
import com.itv.scalapact.plugin.stubber.ScalaPactStubberCommand
import com.itv.scalapact.plugin.tester.ScalaPactTestCommand
import com.itv.scalapact.plugin.verifier.ScalaPactVerifyCommand
import sbt.Keys._
import sbt._

import scala.language.implicitConversions

object ScalaPactPlugin extends Plugin {

  val providerStates = SettingKey[Seq[(String, String => Boolean)]]("provider-states", "A list of provider state setup functions")
  val pactBrokerAddress = SettingKey[String]("pactBrokerAddress", "The base url to publish / pull pact contract files to and from.")
  val providerName = SettingKey[String]("providerName", "The name of the service to verify")
  val consumerNames = SettingKey[Seq[String]]("consumerNames", "The names of the services that consume the service to verify")
  val pactContractVersion = SettingKey[String]("pactContractVersion", "The version number the pact contract will be published under. If missing or empty, the project version will be used.")
  val allowSnapshotPublish = SettingKey[Boolean]("allowSnapshotPublish", "Flag to permit publishing of snapshot pact files to pact broker. Default is false.")


  private val pactSettings = Seq(
    providerStates := Seq(("default", (key: String) => true)),
    pactBrokerAddress := "",
    providerName := "",
    consumerNames := Seq.empty[String],
    pactContractVersion := "",
    allowSnapshotPublish := false
  )

  override lazy val settings = Seq(
    commands += ScalaPactTestCommand.pactTestCommandHyphen,
    commands += ScalaPactTestCommand.pactTestCommandCamel,
    commands += ScalaPactPublishCommand.pactPublishCommandHyphen,
    commands += ScalaPactPublishCommand.pactPublishCommandCamel,
    commands += ScalaPactVerifyCommand.pactVerifyCommandHyphen,
    commands += ScalaPactVerifyCommand.pactVerifyCommandCamel,
    commands += ScalaPactStubberCommand.pactStubberCommandHyphen,
    commands += ScalaPactStubberCommand.pactStubberCommandCamel
  ) ++ pactSettings
}

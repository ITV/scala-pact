package com.itv.scalapact.plugin.verifier

import sbt._

object ScalaPactVerifyCommand {

  //TODO: change to Command.args for a baseUrl and source dir argument
  lazy val pactVerifyCommandHyphen: Command = Command.command("pact-verify")(pactVerify)
  lazy val pactVerifyCommandCamel: Command = Command.command("pactVerify")(pactVerify)

  private lazy val pactVerify: State => State = state => {

    println("Placeholder for ScalaPact verification command")

    // Load a bunch of pact files from a target dir

    // Replay all requests against a baseUrl

    // Match each response against the expected response

    // Report

    state
  }
}

package com.itv.scalapact.plugin.verifier

import com.itv.scalapact.plugin.common.CommandArguments._
import com.itv.scalapact.plugin.common.LocalPactFileLoader._
import sbt._

object ScalaPactVerifyCommand {

  lazy val pactVerifyCommandHyphen: Command = Command.args("pact-verify", "<options>")(pactVerify)
  lazy val pactVerifyCommandCamel: Command = Command.args("pactVerify", "<options>")(pactVerify)

  private lazy val pactVerify: (State, Seq[String]) => State = (state, args) => {

    println("Placeholder for ScalaPact verification command")

    // Replay all requests against a baseUrl

    // Match each response against the expected response

    // Report
    (parseArguments andThen loadPactFiles("pacts"))(args)// andThen verify

    state
  }
}

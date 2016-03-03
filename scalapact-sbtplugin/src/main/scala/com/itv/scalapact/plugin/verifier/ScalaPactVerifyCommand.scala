package com.itv.scalapact.plugin.verifier

import com.itv.scalapact.plugin.common.CommandArguments._
import com.itv.scalapact.plugin.common.LocalPactFileLoader._
import sbt._

import Verifier._

object ScalaPactVerifyCommand {

  lazy val pactVerifyCommandHyphen: Command = Command.args("pact-verify", "<options>")(pactVerify)
  lazy val pactVerifyCommandCamel: Command = Command.args("pactVerify", "<options>")(pactVerify)

  private lazy val pactVerify: (State, Seq[String]) => State = (state, args) => {

    println("Placeholder for ScalaPact verification command")

    (parseArguments andThen loadPactFiles("pacts") andThen verify)(args)

    state
  }
}

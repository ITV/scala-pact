package com.itv.scalapact.plugin.verifier

import com.itv.scalapact.plugin.common.CommandArguments._
import com.itv.scalapact.plugin.common.LocalPactFileLoader._
import com.itv.scalapact.plugin.common.Rainbow._
import sbt._

import Verifier._

object ScalaPactVerifyCommand {

  lazy val pactVerifyCommandHyphen: Command = Command.args("pact-verify", "<options>")(pactVerify)
  lazy val pactVerifyCommandCamel: Command = Command.args("pactVerify", "<options>")(pactVerify)

  private lazy val pactVerify: (State, Seq[String]) => State = (state, args) => {

    println("*************************************".white.bold)
    println("** ScalaPact: Running Verifier     **".white.bold)
    println("*************************************".white.bold)

    (parseArguments andThen loadPactFiles("pacts") andThen verify)(args)

    state
  }
}

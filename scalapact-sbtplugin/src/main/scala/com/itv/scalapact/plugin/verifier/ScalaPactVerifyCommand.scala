package com.itv.scalapact.plugin.verifier

import com.itv.scalapact.plugin.ScalaPactPlugin
import com.itv.scalapactcore.common.CommandArguments._
import com.itv.scalapactcore.common.ColourOuput._
import com.itv.scalapactcore.verifier._
import sbt._

import scala.language.implicitConversions

import Verifier._

object ScalaPactVerifyCommand {

  lazy val pactVerifyCommandHyphen: Command = Command.args("pact-verify", "<options>")(pactVerify)
  lazy val pactVerifyCommandCamel: Command = Command.args("pactVerify", "<options>")(pactVerify)

  implicit def pStateConversion(ps: Seq[(String, String => Boolean)]): List[ProviderState] =
    ps.toList.map(p => ProviderState(p._1, p._2))

  private lazy val pactVerify: (State, Seq[String]) => State = (state, args) => {

    println("*************************************".white.bold)
    println("** ScalaPact: Running Verifier     **".white.bold)
    println("*************************************".white.bold)

    val combinedPactStates = combineProviderStates(Project.extract(state).get(ScalaPactPlugin.providerStates), Project.extract(state).get(ScalaPactPlugin.providerStateMatcher))

    val pactVerifySettings = PactVerifySettings(
      providerStates = combinedPactStates,
      pactBrokerAddress = Project.extract(state).get(ScalaPactPlugin.pactBrokerAddress),
      projectVersion = Project.extract(state).get(Keys.version),
      providerName = Project.extract(state).get(ScalaPactPlugin.providerName),
      consumerNames = Project.extract(state).get(ScalaPactPlugin.consumerNames).toList,
      versionedConsumerNames =
        Project.extract(state).get(ScalaPactPlugin.versionedConsumerNames).toList
        .map(t => VersionedConsumer(t._1, t._2))
    )

    val successfullyVerified = (parseArguments andThen verify(pactVerifySettings)) (args)

    if(successfullyVerified) sys.exit(0) else sys.exit(1)

    state
  }

  def combineProviderStates(directPactStates: Seq[(String, String => Boolean)], patternMatchedStates: PartialFunction[String, Boolean]): PartialFunction[String, Boolean] = {
    val mappedDirectPactStates: Seq[PartialFunction[String, Boolean]] = directPactStates.map{ case (key, handler) =>
      PartialFunction(handler)
    }

    val defaultHandler: PartialFunction[String, Boolean] = {
      case _ => false
    }

    mappedDirectPactStates.fold[PartialFunction[String, Boolean]](PartialFunction(patternMatchedStates)){
      case (matcher,  acc) => acc orElse matcher
    } orElse defaultHandler
  }
}

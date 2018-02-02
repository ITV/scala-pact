package com.itv.scalapact.plugin.verifier

import com.itv.scalapact.plugin.ScalaPactPlugin
import com.itv.scalapactcore.verifier.ProviderState
import com.itv.scalapact.shared.ColourOuput._
import com.itv.scalapact.shared.{ScalaPactSettings, SslContextMap}
import com.itv.scalapactcore.common.LocalPactFileLoader
import com.itv.scalapactcore.verifier._
import sbt._

import scala.language.implicitConversions
import com.itv.scalapactcore.verifier.Verifier._
import com.itv.scalapactcore.common.PactReaderWriter._
import com.itv.scalapact.shared.PactLogger

object ScalaPactVerifyCommand {

  lazy val pactVerifyCommandHyphen: Command = Command.args("pact-verify", "<options>")(pactVerify)
  lazy val pactVerifyCommandCamel: Command = Command.args("pactVerify", "<options>")(pactVerify)

  implicit def pStateConversion(ps: Seq[(String, String => Boolean)]): List[ProviderState] =
    ps.toList.map(p => ProviderState(p._1, p._2))

  private lazy val pactVerify: (State, Seq[String]) => State = (state, args) => {

    doPactVerify(
      Project.extract(state).get(ScalaPactPlugin.autoImport.scalaPactEnv).toSettings + ScalaPactSettings.parseArguments(args),
      Project.extract(state).get(ScalaPactPlugin.autoImport.providerStates),
      Project.extract(state).get(ScalaPactPlugin.autoImport.providerStateMatcher),
      Project.extract(state).get(ScalaPactPlugin.autoImport.pactBrokerAddress),
      Project.extract(state).get(Keys.version),
      Project.extract(state).get(ScalaPactPlugin.autoImport.providerName),
      Project.extract(state).get(ScalaPactPlugin.autoImport.consumerNames),
      Project.extract(state).get(ScalaPactPlugin.autoImport.versionedConsumerNames),
      Project.extract(state).get(ScalaPactPlugin.autoImport.scalaPactSslMap)
    )

    state
  }

  def doPactVerify(scalaPactSettings: ScalaPactSettings, providerStates: Seq[(String, String => Boolean)], providerStateMatcher: PartialFunction[String, Boolean], pactBrokerAddress: String, projectVersion: String, providerName: String, consumerNames: Seq[String], versionedConsumerNames: Seq[(String, String)], sslContextMap: SslContextMap): Unit = {
    PactLogger.message(s"sslContext in doPactVerify $sslContextMap")
    PactLogger.message("")

    PactLogger.message("*************************************".white.bold)
    PactLogger.message("** ScalaPact: Running Verifier     **".white.bold)
    PactLogger.message("*************************************".white.bold)

    val combinedPactStates = combineProviderStatesIntoTotalFunction(providerStates, providerStateMatcher)

    val pactVerifySettings = PactVerifySettings(
      combinedPactStates,
      pactBrokerAddress,
      projectVersion,
      providerName,
      consumerNames.toList,
      versionedConsumerNames =
        versionedConsumerNames.toList
          .map(t => VersionedConsumer(t._1, t._2))
    )

    PactLogger.message(s"about to verify $sslContextMap")
    val successfullyVerified = verify(LocalPactFileLoader.loadPactFiles(pactReader)(true), pactVerifySettings)(pactReader, sslContextMap)(scalaPactSettings)
    PactLogger.message(s"Exiting with  $successfullyVerified")

    if (successfullyVerified) sys.exit(0) else sys.exit(1)

  }

  def combineProviderStatesIntoTotalFunction(directPactStates: Seq[(String, String => Boolean)], patternMatchedStates: PartialFunction[String, Boolean]): String => Boolean = {
    val l = directPactStates
      .map { ps => {
        case s: String if s == ps._1 => ps._2(ps._1)
      }: PartialFunction[String, Boolean]
      }

    l match {
      case Nil =>
        patternMatchedStates orElse { case _: String => false }

      case x :: xs =>
        xs.foldLeft(x)(_ orElse _) orElse patternMatchedStates orElse { case _: String => false }

    }
  }
}

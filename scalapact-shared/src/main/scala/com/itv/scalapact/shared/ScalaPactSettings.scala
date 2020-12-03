package com.itv.scalapact.shared

import com.itv.scalapact.shared.utils.{Helpers, PactLogger}

import scala.concurrent.duration.{Duration, SECONDS}
import scala.util.Properties

//Command line settings - these should override any corresponding sbt settings
case class ScalaPactSettings(
    protocol: Option[String],
    host: Option[String],
    port: Option[Int],
    localPactFilePath: Option[String],
    strictMode: Option[Boolean],
    clientTimeout: Option[Duration],
    outputPath: Option[String],
    publishResultsEnabled: Option[BrokerPublishData],
    pendingPactSettings: Option[PendingPactSettings]
) {
  val giveHost: String            = host.getOrElse("localhost")
  val giveProtocol: String        = protocol.getOrElse("http")
  val givePort: Int               = port.getOrElse(1234)
  val giveStrictMode: Boolean     = strictMode.getOrElse(false)
  val giveClientTimeout: Duration = clientTimeout.getOrElse(Duration(1, SECONDS))
  val giveOutputPath: String      = outputPath.getOrElse(Properties.envOrElse("pact.rootDir", "target/pacts"))

  def +(other: ScalaPactSettings): ScalaPactSettings =
    ScalaPactSettings.append(this, other)

  def withProtocol(protocol: String): ScalaPactSettings =
    this.copy(protocol = Option(protocol))

  def withHost(host: String): ScalaPactSettings =
    this.copy(host = Option(host))

  def withPort(port: Int): ScalaPactSettings =
    this.copy(port = Option(port))

  def withLocalPactFilePath(path: String): ScalaPactSettings =
    this.copy(localPactFilePath = Option(path))

  def enableStrictMode: ScalaPactSettings =
    this.copy(strictMode = Option(true))

  def disableStrictMode: ScalaPactSettings =
    this.copy(strictMode = Option(false))

  def withClientTimeOut(duration: Duration): ScalaPactSettings =
    this.copy(clientTimeout = Option(duration))

  def withOutputPath(outputPath: String): ScalaPactSettings =
    this.copy(outputPath = Option(outputPath))

  def enablePublishResults(providerVersion: String, buildUrl: Option[String]): ScalaPactSettings =
    this.copy(publishResultsEnabled = Option(BrokerPublishData(providerVersion, buildUrl)))

  def toArguments: Map[String, String] =
    List(
      protocol.map(p => ("--protocol", p)),
      host.map(p => ("--host", p)),
      port.map(p => ("--port", p.toString)),
      localPactFilePath.map(p => ("--source", p)),
      strictMode.map(p => ("--strict", p.toString)),
      clientTimeout.map(p => ("--clientTimeout", p.toSeconds.toString)),
      outputPath.map(p => ("--out", p))
    ).collect { case Some(s) => s }.toMap

  def renderAsString: String =
    toArguments.mkString(", ")
}

object ScalaPactSettings {

  def apply: ScalaPactSettings = default

  def default: ScalaPactSettings =
    ScalaPactSettings(None, None, None, None, None, None, None, None, None)

  val parseArguments: Seq[String] => ScalaPactSettings = args => (Helpers.pair andThen convertToArguments)(args.toList)

  def append(a: ScalaPactSettings, b: ScalaPactSettings): ScalaPactSettings =
    ScalaPactSettings(
      host = b.host.orElse(a.host),
      protocol = b.protocol.orElse(a.protocol),
      port = b.port.orElse(a.port),
      localPactFilePath = b.localPactFilePath.orElse(a.localPactFilePath),
      strictMode = b.strictMode.orElse(a.strictMode),
      clientTimeout = b.clientTimeout.orElse(a.clientTimeout),
      outputPath = b.outputPath.orElse(a.outputPath),
      publishResultsEnabled = b.publishResultsEnabled.orElse(a.publishResultsEnabled),
      pendingPactSettings = a.pendingPactSettings.orElse(b.pendingPactSettings)
    )

  private lazy val convertToArguments: Map[String, String] => ScalaPactSettings = argMap =>
    ScalaPactSettings(
      host = argMap.get("--host"),
      protocol = argMap.get("--protocol"),
      port = argMap.get("--port").flatMap(Helpers.safeStringToInt),
      localPactFilePath = argMap.get("--source"),
      strictMode = argMap.get("--strict").flatMap(Helpers.safeStringToBoolean),
      clientTimeout =
        argMap.get("--clientTimeout").flatMap(Helpers.safeStringToLong).flatMap(i => Option(Duration(i, SECONDS))),
      outputPath = argMap.get("--out"),
      publishResultsEnabled = calculateBrokerPublishData(argMap),
      pendingPactSettings = calculatePendingPactSettings(argMap)
    )

  private def calculateBrokerPublishData(argMap: Map[String, String]): Option[BrokerPublishData] = {
    val buildUrl = argMap.get("--publishResultsBuildUrl")
    argMap.get("--publishResultsVersion").map(BrokerPublishData(_, buildUrl))
  }

  private def calculatePendingPactSettings(argMap: Map[String, String]): Option[PendingPactSettings] = {
    val wipSince      = argMap.get("--includeWipPactsSince").flatMap(Helpers.safeStringToDateTime)
    val enablePending = argMap.get("--enablePending").flatMap(Helpers.safeStringToBoolean)
    (wipSince, enablePending) match {
      case (Some(wipSince), Some(false)) =>
        PactLogger.warn(
          "WIP pacts cannot be retrieved if --enablePending is set to false. Setting --enablePending to true."
        )
        Some(PendingPactSettings.IncludeWipPacts(wipSince))
      case (Some(wipSince), _) =>
        Some(PendingPactSettings.IncludeWipPacts(wipSince))
      case (_, Some(true)) =>
        Some(PendingPactSettings.PendingEnabled)
      case (_, Some(false)) => Some(PendingPactSettings.PendingDisabled)
      case (_, _)           => None
    }
  }
}

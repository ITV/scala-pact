package com.itv.scalapact.plugin

import com.itv.scalapact.shared.{BrokerPublishData, ScalaPactSettings}

import scala.concurrent.duration._

case class ScalaPactEnv(
    protocol: Option[String],
    host: Option[String],
    port: Option[Int],
    localPactFilePath: Option[String],
    strictMode: Option[Boolean],
    clientTimeout: Option[Duration],
    outputPath: Option[String],
    publishResultsEnabled: Option[BrokerPublishData],
    enablePending: Option[Boolean]
) {
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

  def enablePublishResults(providerVersion: String, buildUrl: Option[String]): ScalaPactEnv =
    this.copy(publishResultsEnabled = Option(BrokerPublishData(providerVersion, buildUrl)))

  def enablePendingStatus: ScalaPactEnv = this.copy(enablePending = Some(true))

  def toSettings: ScalaPactSettings =
    ScalaPactSettings(
      protocol,
      host,
      port,
      localPactFilePath,
      strictMode,
      clientTimeout,
      outputPath,
      publishResultsEnabled,
      enablePending
    )

}

object ScalaPactEnv {

  def apply: ScalaPactEnv = defaults

  def apply(protocol: String, host: String, port: Int): ScalaPactEnv =
    ScalaPactEnv(
      Option(protocol),
      Option(host),
      Option(port),
      None, // "pacts"
      None, // false
      Option(Duration(1, SECONDS)),
      None, // "target/pacts"
      None, // false
      None  // false
    )

  def defaults: ScalaPactEnv =
    ScalaPactEnv("http", "localhost", 1234)

  def empty: ScalaPactEnv = ScalaPactEnv(None, None, None, None, None, None, None, None, None)

  def append(a: ScalaPactEnv, b: ScalaPactEnv): ScalaPactEnv =
    ScalaPactEnv(
      host = b.host.orElse(a.host),
      protocol = b.protocol.orElse(a.protocol),
      port = b.port.orElse(a.port),
      localPactFilePath = b.localPactFilePath.orElse(a.localPactFilePath),
      strictMode = b.strictMode.orElse(a.strictMode),
      clientTimeout = b.clientTimeout.orElse(a.clientTimeout),
      outputPath = b.outputPath.orElse(a.outputPath),
      publishResultsEnabled = b.publishResultsEnabled.orElse(a.publishResultsEnabled),
      enablePending = b.enablePending.orElse(a.enablePending)
    )
}

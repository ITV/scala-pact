package com.itv.scalapact.shared

import scala.concurrent.duration.{Duration, SECONDS}

case class Arguments(host: Option[String], protocol: Option[String], port: Option[Int], localPactPath: Option[String], strictMode: Option[Boolean], clientTimeout: Option[Int]) {
  val giveHost: String = host.getOrElse("localhost")
  val giveProtocol: String = protocol.getOrElse("http")
  val givePort: Int = port.getOrElse(1234)
  val giveStrictMode: Boolean = strictMode.getOrElse(false)
  val giveClientTimeoutInSeconds: Duration = clientTimeout.map(s => Duration(s, SECONDS)).getOrElse(Duration(1, SECONDS))
}

package com.itv.scalapact

import com.itv.scalapact.circe13.JsonInstances
import com.itv.scalapact.http4s21.impl.{HttpInstances, PactStubber}
import com.itv.scalapact.shared.http.SslContextMap
import com.itv.scalapact.shared.IPactStubber
import com.itv.scalapactcore.common.stubber.InteractionManager
import org.scalatest._

trait PactForgerSuite extends ScalaPactForgerDslMixin with HttpInstances with JsonInstances with Suite {
  self: Suite =>

  implicit val stubber: IPactStubber = new PactStubber(new InteractionManager)
  implicit val sslContextMap: SslContextMap = SslContextMap.defaultEmptyContextMap

  def sslContextName: Option[String] = None
  def port: Option[Int] = None
  def connectionPoolSize: Int = 5

  abstract override def run(testName: Option[String], args: Args): Status = {
    if (expectedTestCount(args.filter) == 0) {
      new CompositeStatus(Set.empty)
    } else {
      stubber.start(connectionPoolSize, sslContextName, port)
      try {
        super.run(testName, args)
      } finally {
        stubber.shutdown()
      }
    }
  }
}

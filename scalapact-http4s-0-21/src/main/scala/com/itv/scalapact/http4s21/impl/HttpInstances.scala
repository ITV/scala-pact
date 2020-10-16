package com.itv.scalapact.http4s21.impl

import com.itv.scalapact.shared.IResultPublisher
import com.itv.scalapact.shared.typeclasses.IPactStubber

trait HttpInstances {

  // Note that we create a new stubber anytime this implicit is needed (i.e. this is a `def`).
  // We need this because implementations of `IPactStubber` might want to have their own state about the server running.
  implicit def serverInstance: IPactStubber =
    new PactStubber

  implicit val resultPublisher: IResultPublisher = new ResultPublisher(Http4sClientHelper.doRequest)
}

package com.itv.scalapact.circe13

import com.itv.scalapact.shared.typeclasses.{IPactReader, IPactWriter}

trait JsonInstances {
  implicit val pactReaderInstance: IPactReader = new PactReader

  implicit val pactWriterInstance: IPactWriter = new PactWriter
}

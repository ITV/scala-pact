package com.itv.scalapact.circe13

import com.itv.scalapact.shared.json.{IPactReader, IPactWriter}

trait JsonInstances {
  implicit val pactReaderInstance: IPactReader = new PactReader

  implicit val pactWriterInstance: IPactWriter = new PactWriter
}

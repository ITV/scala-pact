package com.itv.scalapact

import com.itv.scalapact.circe12.{PactReader, PactWriter}
import com.itv.scalapact.shared.typeclasses.{IPactReader, IPactWriter}

package object json {
  implicit val pactReaderInstance: IPactReader = new PactReader

  implicit val pactWriterInstance: IPactWriter = new PactWriter

  val JsonConversionFunctions: circe12.JsonConversionFunctions.type = circe12.JsonConversionFunctions
}

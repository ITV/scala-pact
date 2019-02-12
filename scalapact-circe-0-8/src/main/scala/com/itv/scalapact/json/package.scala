package com.itv.scalapact

import com.itv.scalapact.circe08.{PactReader, PactWriter}
import com.itv.scalapact.shared.typeclasses.{IPactReader, IPactWriter}

package object json {
  implicit val pactReaderInstance: IPactReader =
    new PactReader

  implicit val pactWriterInstance: IPactWriter =
    new PactWriter

  val JsonConversionFunctions: circe08.JsonConversionFunctions.type = circe08.JsonConversionFunctions
}

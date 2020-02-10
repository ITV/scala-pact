package com.itv.scalapact

import com.itv.scalapact.shared.typeclasses.{IPactReader, IPactWriter}

package object circe13 {

  implicit val pactReaderInstance: IPactReader =
    new PactReader

  implicit val pactWriterInstance: IPactWriter =
    new PactWriter

}

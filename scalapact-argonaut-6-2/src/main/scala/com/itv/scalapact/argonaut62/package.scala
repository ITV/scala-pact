package com.itv.scalapact

import com.itv.scalapact.shared.json.{IPactReader, IPactWriter}

package object argonaut62 {
  implicit val pactReaderInstance: IPactReader =
    new PactReader

  implicit val pactWriterInstance: IPactWriter =
    new PactWriter
}

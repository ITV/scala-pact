package com.itv.scalapact

import com.itv.scalapact.argonaut62.{PactReader, PactWriter}
import com.itv.scalapact.shared.json.{IPactReader, IPactWriter}

package object json {
  implicit val pactReaderInstance: IPactReader =
    new PactReader

  implicit val pactWriterInstance: IPactWriter =
    new PactWriter
}

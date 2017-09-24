package com.itv.scalapactcore.common

import com.itv.scalapact.shared.{IPactReader, IPactWriter}
import com.itv.scalapact.shared.pact.{PactReader, PactWriter}

object PactReaderWriter {

  type ReadPactF = PactReader.ReadPactF
  type WritePactF = PactWriter.WritePactF

  val readPact: PactReader.ReadPactF = PactReader.readPact
  val writePact: PactWriter.WritePactF = PactWriter.writePact

  implicit val pactReader: IPactReader = PactReader
  implicit val pactWriter: IPactWriter = PactWriter

}

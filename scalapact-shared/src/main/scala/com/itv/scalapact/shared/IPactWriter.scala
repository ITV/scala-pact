package com.itv.scalapact.shared

trait IPactWriter {

  type WritePactF = Pact => String

  val writePact: WritePactF = p => pactToJsonString(p)

  def pactToJsonString(pact: Pact): String

}

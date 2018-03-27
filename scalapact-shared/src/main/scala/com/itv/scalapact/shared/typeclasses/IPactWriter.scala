package com.itv.scalapact.shared.typeclasses

import com.itv.scalapact.shared.Pact

trait IPactWriter {

  type WritePactF = Pact => String

  val writePact: WritePactF = p => pactToJsonString(p)

  def pactToJsonString(pact: Pact): String

}

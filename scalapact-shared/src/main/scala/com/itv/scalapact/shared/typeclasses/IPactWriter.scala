package com.itv.scalapact.shared.typeclasses

import com.itv.scalapact.shared.{ConsumerVersionSelector, Pact}

trait IPactWriter {

  type WritePactF = (Pact, String) => String

  val writePact: WritePactF = (p, v) => pactToJsonString(p, v)

  def pactToJsonString(pact: Pact, scalaPactVersion: String): String

  def consumerVersionSelectorsToJsonString(selectors: List[ConsumerVersionSelector], providerVersionTags: List[String]): String
}

package com.itv.scalapact.shared.typeclasses

import com.itv.scalapact.shared.{ConsumerVersionSelector, Pact}

trait IPactWriter {
  def pactToJsonString(pact: Pact, scalaPactVersion: String): String
  def consumerVersionSelectorsToJsonString(selectors: List[ConsumerVersionSelector], providerVersionTags: List[String]): String
}

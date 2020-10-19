package com.itv.scalapact.model

object ScalaPactResponse {
  val default: ScalaPactResponse = ScalaPactResponse(200, Map.empty, None, None)
}

case class ScalaPactResponse(status: Int,
                             headers: Map[String, String],
                             body: Option[String],
                             matchingRules: Option[List[ScalaPactMatchingRule]])

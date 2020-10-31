package com.itv.scalapact.model

import com.itv.scalapact.shared.http.HttpMethod

case class ScalaPactRequest(
    method: HttpMethod,
    path: String,
    query: Option[String],
    headers: Map[String, String],
    body: Option[String],
    matchingRules: Option[List[ScalaPactMatchingRule]]
)

object ScalaPactRequest {
  val default: ScalaPactRequest = ScalaPactRequest(HttpMethod.GET, "/", None, Map.empty, None, None)
}

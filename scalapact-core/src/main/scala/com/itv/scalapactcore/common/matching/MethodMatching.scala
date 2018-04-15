package com.itv.scalapactcore.common.matching

object MethodMatching {

  def matchMethods(expected: Option[String], received: Option[String]): MatchOutcome =
    GeneralMatcher.generalMatcher(
      expected,
      received,
      MatchOutcomeFailed("Methods did not match", 50),
      (e: String, r: String) =>
        if (e.toUpperCase == r.toUpperCase) MatchOutcomeSuccess
        else MatchOutcomeFailed(s"Method '${e.toUpperCase}' did not match '${r.toUpperCase}'", 50)
    )

}

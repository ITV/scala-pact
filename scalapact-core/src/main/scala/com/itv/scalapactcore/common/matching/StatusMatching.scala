package com.itv.scalapactcore.common.matching

object StatusMatching {

  def matchStatusCodes(expected: Option[Int], received: Option[Int]): MatchOutcome =
    GeneralMatcher.generalMatcher(
      expected,
      received,
      MatchOutcomeFailed("Status codes did not match", 50),
      (e: Int, r: Int) =>
        if (e == r) MatchOutcomeSuccess else MatchOutcomeFailed(s"Status code '$e' did not match '$r'", 50)
    )

}

package com.itv.scalapactcore.common.matching

object GeneralMatcher {

  def generalMatcher[A](
      expected: Option[A],
      received: Option[A],
      defaultFailure: MatchOutcomeFailed,
      predicate: (A, A) => MatchOutcome
  ): MatchOutcome =
    (expected, received) match {
      case (None, None) => MatchOutcomeSuccess

      case (Some(null), Some(null)) => MatchOutcomeSuccess
      case (None, Some(null))       => MatchOutcomeSuccess
      case (Some(null), None)       => MatchOutcomeSuccess

      case (Some("null"), Some("null")) => MatchOutcomeSuccess
      case (None, Some("null"))         => MatchOutcomeSuccess
      case (Some("null"), None)         => MatchOutcomeSuccess

      case (None, Some(_))    => MatchOutcomeSuccess
      case (Some(_), None)    => defaultFailure
      case (Some(e), Some(r)) => predicate(e, r)
    }

}

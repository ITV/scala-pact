package com.itv.scalapactcore.common.matching

sealed trait MatchOutcome {
  val isSuccess: Boolean

  def append(other: MatchOutcome): MatchOutcome =
    (this, other) match {
      case (MatchOutcomeSuccess, MatchOutcomeSuccess) =>
        MatchOutcomeSuccess

      case (MatchOutcomeSuccess, r @ MatchOutcomeFailed(_)) =>
        r

      case (l @ MatchOutcomeFailed(_), MatchOutcomeSuccess) =>
        l

      case (MatchOutcomeFailed(diffsA), MatchOutcomeFailed(diffsB)) =>
        MatchOutcomeFailed(diffsA ++ diffsB)
    }

  def +(other: MatchOutcome): MatchOutcome = append(other)

  def renderAsString: String =
    this match {
      case MatchOutcomeSuccess =>
        "Match success"

      case n: MatchOutcomeFailed =>
        n.renderDifferences
    }
}

object MatchOutcome {
  def indentity: MatchOutcome = MatchOutcomeSuccess
}

case object MatchOutcomeSuccess extends MatchOutcome {
  val isSuccess: Boolean = true
}
case class MatchOutcomeFailed(differences: List[String]) extends MatchOutcome {
  val isSuccess: Boolean = false

  def renderDifferences: String = differences.mkString("\n")
}

object MatchOutcomeFailed {
  def apply(message: String): MatchOutcomeFailed =
    MatchOutcomeFailed(List(message))
}

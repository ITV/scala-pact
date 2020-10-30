package com.itv.scalapactcore.common.matching

sealed trait MatchOutcome {
  val isSuccess: Boolean
  val drift: Int

  def append(other: MatchOutcome): MatchOutcome =
    (this, other) match {
      case (MatchOutcomeSuccess, MatchOutcomeSuccess) =>
        MatchOutcomeSuccess

      case (MatchOutcomeSuccess, r @ MatchOutcomeFailed(_, _)) =>
        r

      case (l @ MatchOutcomeFailed(_, _), MatchOutcomeSuccess) =>
        l

      case (MatchOutcomeFailed(diffsA, scoreA), MatchOutcomeFailed(diffsB, scoreB)) =>
        MatchOutcomeFailed(diffsA ++ diffsB, scoreA + scoreB)
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

  val MaxDrift: Int = 100000

  def fromPredicate(p: => Boolean, failureMessage: String, driftAmount: Int): MatchOutcome =
    if (p) MatchOutcomeSuccess
    else MatchOutcomeFailed(failureMessage, driftAmount)
}

case object MatchOutcomeSuccess extends MatchOutcome {
  val drift: Int         = 0
  val isSuccess: Boolean = true
}
case class MatchOutcomeFailed(differences: List[String], drift: Int) extends MatchOutcome {
  val isSuccess: Boolean = false
  val errorCount: Int    = differences.length

  def renderDifferences: String = differences.mkString("\n")
}

object MatchOutcomeFailed {
  def apply(message: String, drift: Int): MatchOutcomeFailed =
    MatchOutcomeFailed(List(message), drift)

  def apply(message: String): MatchOutcomeFailed =
    MatchOutcomeFailed(List(message), MatchOutcome.MaxDrift)
}

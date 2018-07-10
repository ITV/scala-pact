package com.itv.scalapact.shared.typeclasses

import com.itv.scalapact.shared.MatchingRule

trait IInferTypes[T] {
  def infer(t: T): Map[String, MatchingRule] =
    inferFrom(t).mapValues(
      v => MatchingRule(Some(v), None, None)
    )

  protected def inferFrom(t: T): Map[String, String]
}

object IInferTypes {
  implicit def noneInferTypeInstance[T] = new IInferTypes[T] {
    override protected def inferFrom(t: T): Map[String, String] = Map.empty
  }
}

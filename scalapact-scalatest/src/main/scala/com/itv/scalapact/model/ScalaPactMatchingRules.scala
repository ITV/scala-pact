package com.itv.scalapact.model

sealed trait ScalaPactMatchingRule {
  val key: String
}

object ScalaPactMatchingRule {

  final case class ScalaPactMatchingRuleRegex(key: String, regex: String) extends ScalaPactMatchingRule

  final case class ScalaPactMatchingRuleType(key: String) extends ScalaPactMatchingRule

  final case class ScalaPactMatchingRuleArrayMinLength(key: String, minimum: Int) extends ScalaPactMatchingRule

}

final case class ScalaPactMatchingRules(rules: List[ScalaPactMatchingRule]) {
  def ~>(newRules: ScalaPactMatchingRules): ScalaPactMatchingRules = ScalaPactMatchingRules(
    rules = rules ++ newRules.rules
  )
  def toOption: Option[List[ScalaPactMatchingRule]] = rules match {
    case Nil => None
    case rs  => Some(rs)
  }
}

object ScalaPactMatchingRules {
  def empty: ScalaPactMatchingRules = ScalaPactMatchingRules(Nil)
}

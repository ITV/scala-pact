package com.itv.scalapactcore.common.matching

import com.itv.scalapactcore.MatchingRule

sealed trait ArrayMatchingStatus extends Product with Serializable
case object RuleMatchSuccess extends ArrayMatchingStatus
case object RuleMatchFailure extends ArrayMatchingStatus
case object NoRuleMatchRequired extends ArrayMatchingStatus

case class MatchingRuleContext(path: String, rule: MatchingRule)

object ArrayMatchingStatus {

  val listArrayMatchStatusToSingle: List[ArrayMatchingStatus] => ArrayMatchingStatus = {
    case l: List[ArrayMatchingStatus] if l.contains(RuleMatchFailure) => RuleMatchFailure
    case l: List[ArrayMatchingStatus] if l.contains(RuleMatchSuccess) => RuleMatchSuccess
    case _ => NoRuleMatchRequired
  }
}

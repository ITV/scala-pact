package com.itv.scalapactcore.common

import com.itv.scalapact.shared.matchir.{IrNodeEqualityResult, IrNodeMatchingRules, IrNodesEqual, IrNodesNotEqual}

package object matching {
  def nodeMatchToMatchResult(irNodeEqualityResult: IrNodeEqualityResult,
                             rules: IrNodeMatchingRules,
                             isXml: Boolean): MatchOutcome =
    irNodeEqualityResult match {
      case IrNodesEqual =>
        MatchOutcomeSuccess

      case e: IrNodesNotEqual =>
        MatchOutcomeFailed(e.renderDifferencesListWithRules(rules, isXml), e.differences.length * 1)
    }
}

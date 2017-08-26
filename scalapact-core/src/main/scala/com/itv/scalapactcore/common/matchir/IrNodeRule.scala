package com.itv.scalapactcore.common.matchir

import com.itv.scalapactcore.MatchingRule
import com.itv.scalapactcore.common.matchir.PactPathParseResult.{PactPathParseFailure, PactPathParseSuccess}

import scala.util.matching.Regex

case class IrNodeMatchingRules(rules: List[IrNodeRule]) {

  def +(other: IrNodeMatchingRules): IrNodeMatchingRules =
    IrNodeMatchingRules(rules ++ other.rules)

  def findForPath(path: IrNodePath): Option[IrNodeRule] =
    rules.find(_.path === path)

  def validateNode(path: IrNodePath, expected: IrNode, actual: IrNode): Option[IrNodeEqualityResult] = {
    findForPath(path).map {
      case IrNodeTypeRule(_) =>
        ???

      case IrNodeRegexRule(regex, _) =>
        ???

      case IrNodeMinArrayLengthRule(len, _) =>
        ???
    }
  }

  def validatePrimitive(path: IrNodePath, expected: IrNodePrimitive, actual: IrNodePrimitive): Option[IrNodeEqualityResult] = {
    findForPath(path).map {
      case IrNodeTypeRule(_) =>
        ???

      case IrNodeRegexRule(regex, _) =>
        ???

      case IrNodeMinArrayLengthRule(len, _) =>
        ???
    }
  }

}

object IrNodeMatchingRules {

  def empty: IrNodeMatchingRules = IrNodeMatchingRules(Nil)

  def apply(rule: IrNodeRule): IrNodeMatchingRules = IrNodeMatchingRules(List(rule))

  def apply(rules: IrNodeRule*): IrNodeMatchingRules = IrNodeMatchingRules(rules.toList)

  //TODO: Fails inline and carries on... not sure how I feel about that.
  def fromPactRules(rules: Option[Map[String, MatchingRule]]): IrNodeMatchingRules = {
    val l = rules match {
      case None =>
        List(empty)

      case Some(ruleMap) =>
        ruleMap.toList.map { pair =>
          (IrNodePath.fromPactPath(pair._1), pair._2) match {
            case (e: PactPathParseFailure, _) =>
              println(e.errorString)
              empty

            case (PactPathParseSuccess(path), MatchingRule(Some("type"), _, _)) =>
              IrNodeMatchingRules(IrNodeTypeRule(path))

            case (PactPathParseSuccess(path), MatchingRule(Some("regex"), Some(regex), _)) =>
              IrNodeMatchingRules(IrNodeRegexRule(regex, path))

            case (PactPathParseSuccess(path), MatchingRule(Some("min"), _, Some(len))) =>
              IrNodeMatchingRules(IrNodeMinArrayLengthRule(len, path))

            case _ =>
              println("Failed to read rule: " + pair._2.renderAsString)
              empty
          }
        }
    }

    l.foldLeft(empty)(_ + _)
  }

}

sealed trait IrNodeRule {
  val path: IrNodePath
}
case class IrNodeTypeRule(path: IrNodePath) extends IrNodeRule
case class IrNodeRegexRule(regex: String, path: IrNodePath) extends IrNodeRule
case class IrNodeMinArrayLengthRule(length: Int, path: IrNodePath) extends IrNodeRule

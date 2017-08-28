package com.itv.scalapactcore.common.matchir

import com.itv.scalapactcore.MatchingRule
import com.itv.scalapactcore.common.matchir.PactPathParseResult.{PactPathParseFailure, PactPathParseSuccess}

case class IrNodeMatchingRules(rules: List[IrNodeRule]) {

  def +(other: IrNodeMatchingRules): IrNodeMatchingRules =
    IrNodeMatchingRules(rules ++ other.rules)

  def findForPath(path: IrNodePath): Option[IrNodeRule] =
    rules.find(_.path === path)

  def validateNode(path: IrNodePath, expected: IrNode, actual: IrNode): Option[IrNodeEqualityResult] = {
    findForPath(path).flatMap {
      case IrNodeTypeRule(_) =>
        None

      case IrNodeRegexRule(_, _) =>
        None

      case IrNodeMinArrayLengthRule(len, _) =>
        Option {
          if (actual.arrays.getOrElse(expected.label, Nil).length >= len) IrNodesEqual
          else IrNodesNotEqual(s"Array '${expected.label}' did not meet minimum length requirement of '$len'", path)
        }

    }
  }

  def validatePrimitive(path: IrNodePath, expected: IrNodePrimitive, actual: IrNodePrimitive): Option[IrNodeEqualityResult] = {
    findForPath(path).flatMap {
      case IrNodeTypeRule(_) =>
        Option {
          if (expected.primitiveTypeName == actual.primitiveTypeName) IrNodesEqual
          else IrNodesNotEqual(s"Primitive type '${expected.primitiveTypeName}' did not match actual '${actual.primitiveTypeName}'", path)
        }

      case IrNodeRegexRule(regex, _) if expected.isString && actual.isString =>
        actual.asString.map { str =>
          if(regex.r.findAllIn(str).nonEmpty) IrNodesEqual
          else IrNodesNotEqual(s"String '$str' did not match pattern '$regex'", path)
        }

      case IrNodeMinArrayLengthRule(_, _) =>
        None

      case _ =>
        None
    }
  }

}

object IrNodeMatchingRules {

  implicit val defaultEmptyRules: IrNodeMatchingRules = IrNodeMatchingRules.empty

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

            case (PactPathParseSuccess(path), MatchingRule(None, Some(regex), _)) =>
              IrNodeMatchingRules(IrNodeRegexRule(regex, path))

            case (PactPathParseSuccess(path), MatchingRule(Some("min"), _, Some(len))) =>
              IrNodeMatchingRules(IrNodeMinArrayLengthRule(len, path))

            case (PactPathParseSuccess(path), MatchingRule(None, _, Some(len))) =>
              IrNodeMatchingRules(IrNodeMinArrayLengthRule(len, path))

            case (p, r) =>
              println("Failed to read rule: " + r.renderAsString + s" for path '$p'")
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

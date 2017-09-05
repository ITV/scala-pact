package com.itv.scalapactcore.common.matchir

import com.itv.scalapactcore.MatchingRule
import com.itv.scalapactcore.common.matchir.PactPathParseResult.{PactPathParseFailure, PactPathParseSuccess}

case class IrNodeMatchingRules(rules: List[IrNodeRule]) {

  def +(other: IrNodeMatchingRules): IrNodeMatchingRules =
    IrNodeMatchingRules(rules ++ other.rules)

  def findForPath(path: IrNodePath): List[IrNodeRule] =
    rules.filter(_.path.noText === path.noText)

  def validateNode(path: IrNodePath, expected: IrNode, actual: IrNode): List[IrNodeEqualityResult] = {
    findForPath(path).flatMap {
      case r @ IrNodeTypeRule(_) =>
        (expected.value, actual.value) match {
          case (Some(e), Some(a)) =>
            if(e.primitiveTypeName == a.primitiveTypeName) List(IrNodesEqual)
            else List(IrNodesNotEqual(s"Primitive type '${e.primitiveTypeName}' did not match actual '${a.primitiveTypeName}'", path))

          case (Some(_), None) =>
            List(IrNodesNotEqual(s"Missing actual value, could not check rule: " + r.renderAsString, path))

          case (_, Some(_)) =>
            List(IrNodesNotEqual(s"Missing expected value, could not check rule: " + r.renderAsString, path))

          case (_, _) =>
            Nil
        }

      case IrNodeRegexRule(_, _) =>
        Nil

      case IrNodeMinArrayLengthRule(len, _) =>
        List {
          if (actual.children.length >= len) IrNodesEqual
          else IrNodesNotEqual(s"Array '${expected.label}' did not meet minimum length requirement of '$len'", path)
        }
    }
  }

  def findAncestralTypeRule(path: IrNodePath): List[IrNodeRule] = {
    (path, findForPath(path.parent).find(p => p.isTypeRule).toList) match {
      case (IrNodePathEmpty, l) =>
        l

      case (p, Nil) =>
        findAncestralTypeRule(p.parent)

      case (_, l) =>
        l
    }
  }

  def validatePrimitive(path: IrNodePath, expected: IrNodePrimitive, actual: IrNodePrimitive, checkParentTypeRule: Boolean): List[IrNodeEqualityResult] = {
    val parentTypeRules = if(checkParentTypeRule) findAncestralTypeRule(path) else Nil

    (parentTypeRules ++ findForPath(path)).map {
      case IrNodeTypeRule(_) =>
        Option {
          if (expected.primitiveTypeName == actual.primitiveTypeName) IrNodesEqual
          else IrNodesNotEqual(s"Primitive type '${expected.primitiveTypeName}' did not match actual '${actual.primitiveTypeName}'", path)
        }

      case IrNodeRegexRule(regex, _) if expected.isString && actual.isString =>
        actual.asString.map { str =>
          if (regex.r.findAllIn(str).nonEmpty) IrNodesEqual
          else IrNodesNotEqual(s"String '$str' did not match pattern '$regex'", path)
        }

      case IrNodeMinArrayLengthRule(_, _) =>
        None

      case _ =>
        None
    }.collect { case Some(s) => s }
  }

  def renderAsString: String = s"Rules:\n - ${rules.map(r => r.renderAsString).mkString("\n - ")}"

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

            case (PactPathParseSuccess(path), MatchingRule(Some("type"), None, None)) =>
              IrNodeMatchingRules(IrNodeTypeRule(path))

            case (PactPathParseSuccess(path), MatchingRule(Some("type"), None, Some(len))) =>
              IrNodeMatchingRules(IrNodeTypeRule(path)) + IrNodeMatchingRules(IrNodeMinArrayLengthRule(len, path))

            case (PactPathParseSuccess(path), MatchingRule(Some("type"), Some(regex), Some(len))) =>
              IrNodeMatchingRules(IrNodeTypeRule(path)) + IrNodeMatchingRules(IrNodeRegexRule(regex, path)) + IrNodeMatchingRules(IrNodeMinArrayLengthRule(len, path))

            case (PactPathParseSuccess(path), MatchingRule(Some("regex"), Some(regex), None)) =>
              IrNodeMatchingRules(IrNodeRegexRule(regex, path))

            case (PactPathParseSuccess(path), MatchingRule(Some("regex"), Some(regex), Some(len))) =>
              IrNodeMatchingRules(IrNodeRegexRule(regex, path)) + IrNodeMatchingRules(IrNodeMinArrayLengthRule(len, path))

            case (PactPathParseSuccess(path), MatchingRule(None, Some(regex), None)) =>
              IrNodeMatchingRules(IrNodeRegexRule(regex, path))

            case (PactPathParseSuccess(path), MatchingRule(None, Some(regex), Some(len))) =>
              IrNodeMatchingRules(IrNodeRegexRule(regex, path)) + IrNodeMatchingRules(IrNodeMinArrayLengthRule(len, path))

            case (PactPathParseSuccess(path), MatchingRule(Some("min"), None, Some(len))) =>
              IrNodeMatchingRules(IrNodeMinArrayLengthRule(len, path))

            case (PactPathParseSuccess(path), MatchingRule(Some("min"), Some(regex), Some(len))) =>
              IrNodeMatchingRules(IrNodeRegexRule(regex, path)) + IrNodeMatchingRules(IrNodeMinArrayLengthRule(len, path))

            case (PactPathParseSuccess(path), MatchingRule(None, None, Some(len))) =>
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

  def isTypeRule: Boolean
  def isRegexRule: Boolean
  def isMinArrayLengthRule: Boolean

  def renderAsString: String =
    this match {
      case IrNodeTypeRule(p) =>
        s"""Type rule [${p.renderAsString}]"""

      case IrNodeRegexRule(r, p) =>
        s"""Regex rule [$r] [${p.renderAsString}]"""

      case IrNodeMinArrayLengthRule(l, p) =>
        s"""Min array length rule [$l] [${p.renderAsString}]"""
    }

}
case class IrNodeTypeRule(path: IrNodePath) extends IrNodeRule {
  def isTypeRule: Boolean = true
  def isRegexRule: Boolean = false
  def isMinArrayLengthRule: Boolean = false
}
case class IrNodeRegexRule(regex: String, path: IrNodePath) extends IrNodeRule {
  def isTypeRule: Boolean = false
  def isRegexRule: Boolean = true
  def isMinArrayLengthRule: Boolean = false
}
case class IrNodeMinArrayLengthRule(length: Int, path: IrNodePath) extends IrNodeRule {
  def isTypeRule: Boolean = false
  def isRegexRule: Boolean = false
  def isMinArrayLengthRule: Boolean = true
}

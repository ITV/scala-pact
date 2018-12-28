package com.itv.scalapact.shared.matchir

import scala.language.implicitConversions

case class IrNode(label: String,
                  value: Option[IrNodePrimitive],
                  children: List[IrNode],
                  ns: Option[String],
                  attributes: IrNodeAttributes,
                  isArray: Boolean,
                  isXml: Boolean,
                  path: IrNodePath) {

  import IrNodeEqualityResult._

  def =~(other: IrNode)(implicit rules: IrNodeMatchingRules): IrNodeEqualityResult =
    isEqualTo(other, strict = false, rules, bePermissive = false)
  def =<>=(other: IrNode)(implicit rules: IrNodeMatchingRules,
                          permissive: IrNodeMatchPermissivity): IrNodeEqualityResult =
    isEqualTo(other, strict = true, rules, bePermissive = permissive.bePermissive)

  def isEqualTo(other: IrNode,
                strict: Boolean,
                rules: IrNodeMatchingRules,
                bePermissive: Boolean): IrNodeEqualityResult = {

    val nodeEquality = check[Boolean](nodeType(other.path, this.isXml), this.isArray, other.isArray) +
      check[String](labelTest(other.path), this.label, other.label) +
      check[Option[IrNodePrimitive]](valueTest(strict, this.isXml, other.path, rules, this, other),
                                     this.value,
                                     other.value) +
      check[Option[String]](namespaceTest(other.path), this.ns, other.ns) +
      check[IrNodeAttributes](attributesTest(strict, this.isXml, bePermissive, other.path, rules),
                              this.attributes,
                              other.attributes) +
      check[IrNodePath](pathTest(other.path), this.path, other.path)

    val ruleResults = RuleChecks.checkForNode(rules, other.path, this, other)

    val childEquality = check[List[IrNode]](childrenTest(strict, other.path, isXml, bePermissive, rules, this, other),
                                            this.children,
                                            other.children)

    ruleResults
      .map(_ + childEquality)
      .getOrElse(nodeEquality + childEquality)
  }

  def withNamespace(ns: String): IrNode                    = this.copy(ns = Option(ns))
  def withAttributes(attributes: IrNodeAttributes): IrNode = this.copy(attributes = attributes)
  def withPath(path: IrNodePath): IrNode                   = this.copy(path = path)
  def markAsArray: IrNode                                  = this.copy(isArray = true)
  def markAsXml: IrNode                                    = this.copy(isXml = true)

  def renderAsString: String = renderAsString(0)

  def renderAsString(indent: Int): String = {
    val i = List.fill(indent)("  ").mkString
    val n = ns.map("  namespace: " + _ + "").getOrElse("")
    val v = value.map(v => "  value: " + v.renderAsString).getOrElse("")
    val a =
      if (attributes.attributes.isEmpty) ""
      else s"  attributes: [${attributes.attributes.map(p => p._1 + "=" + p._2.value.renderAsString).mkString(", ")}]"
    val c = if (children.isEmpty) "" else "\n" + children.map(_.renderAsString(indent + 1)).mkString("\n")
    val p = "  " + path.renderAsString
    s"$i- $label$n$v$a$p$c"
  }

}

object IrNodeEqualityResult {

  val nodeType: (IrNodePath, Boolean) => (Boolean, Boolean) => IrNodeEqualityResult =
    (path, isXml) =>
      (a, b) =>
        if (isXml) IrNodesEqual
        else {
          val f = (bb: Boolean) => if (bb) "array" else "object"

          if (a == b) IrNodesEqual
          else IrNodesNotEqual(s"Expected type '${f(a)}' but got '${f(b)}'", path)
    }

  val labelTest: IrNodePath => (String, String) => IrNodeEqualityResult =
    path =>
      (a, b) => {
        if (a == b) IrNodesEqual else IrNodesNotEqual(s"Label '$a' did not match '$b'", path)
    }

  val valueTest: (Boolean, Boolean, IrNodePath, IrNodeMatchingRules, IrNode, IrNode) => (
      Option[IrNodePrimitive],
      Option[IrNodePrimitive]
  ) => IrNodeEqualityResult = { (strict, isXml, path, rules, parentA, parentB) => (a, b) =>
    if (parentA.path.lastSegmentLabel == parentB.path.lastSegmentLabel) {
      val equality: IrNodeEqualityResult = if (strict) {
        (a, b) match {
          case (Some(v1: IrNodePrimitive), Some(v2: IrNodePrimitive)) =>
            if (v1 == v2) IrNodesEqual
            else IrNodesNotEqual(s"Value '${v1.renderAsString}' did not match '${v2.renderAsString}'", path)

          case (Some(v1: IrNodePrimitive), None) =>
            IrNodesNotEqual(s"Value '${v1.renderAsString}' did not match empty value", path)

          case (None, Some(v2: IrNodePrimitive)) =>
            IrNodesNotEqual(s"Empty value did not match '${v2.renderAsString}'", path)

          case (None, None) =>
            IrNodesEqual
        }
      } else {
        (a, b) match {
          case (Some(v1: IrNodePrimitive), Some(v2: IrNodePrimitive)) =>
            if (v1 == v2) IrNodesEqual
            else IrNodesNotEqual(s"Value '${v1.renderAsString}' did not match '${v2.renderAsString}'", path)

          case (Some(v1: IrNodePrimitive), None) =>
            IrNodesNotEqual(s"Value '${v1.renderAsString}' did not match empty value", path)

          case (None, Some(_: IrNodePrimitive)) =>
            IrNodesEqual

          case (None, None) =>
            IrNodesEqual
        }
      }

      RuleChecks.checkForPrimitive(rules, path, a, b, checkParentTypeRule = true, isXml).getOrElse(equality)
    } else {
      IrNodesEqual
    }
  }

  val namespaceTest: IrNodePath => (Option[String], Option[String]) => IrNodeEqualityResult = path => {
    case (Some(v1: String), Some(v2: String)) =>
      if (v1 == v2) IrNodesEqual else IrNodesNotEqual(s"Namespace '$v1' did not match '$v2'", path)

    case (Some(v1: String), None) =>
      IrNodesNotEqual(s"Namespace '$v1' did not match empty namespace", path)

    case (None, Some(v2: String)) =>
      IrNodesNotEqual(s"Empty namespace did not match '$v2'", path)

    case (None, None) =>
      IrNodesEqual
  }

  val pathTest: IrNodePath => (IrNodePath, IrNodePath) => IrNodeEqualityResult = path =>
    (a, b) => {
      val segA = a.lastSegmentLabel
      val segB = b.lastSegmentLabel

      if (segA == segB) IrNodesEqual
      else IrNodesNotEqual(s"Path node '$segA' did not match '$segB'", path)
  }

  implicit private def listOfResultsToResult(l: List[IrNodeEqualityResult]): IrNodeEqualityResult =
    l match {
      case Nil     => IrNodesEqual
      case x :: xs => xs.foldLeft(x)(_ + _)
    }

  private def strictCheckChildren(path: IrNodePath,
                                  strict: Boolean,
                                  bePermissive: Boolean,
                                  rules: IrNodeMatchingRules,
                                  a: List[IrNode],
                                  b: List[IrNode],
                                  ignoreLength: Boolean): IrNodeEqualityResult =
    if (!ignoreLength && a.length != b.length)
      IrNodesNotEqual(s"Differing number of children, cannot check equality. Expected ${a.length} got ${b.length}",
                      path)
    else {
      a.zip(b).map { p =>
        p._1.isEqualTo(p._2, strict, rules, bePermissive)
      }
    }

  private def findClosestMatch(expected: IrNode,
                               possibleMatches: List[IrNode],
                               strict: Boolean,
                               rules: IrNodeMatchingRules,
                               bePermissive: Boolean): IrNodeEqualityResult = {
    def rec(e: IrNode, remaining: List[IrNode], fails: List[IrNodesNotEqual]): IrNodeEqualityResult =
      remaining match {
        case Nil =>
          fails.sortBy(_.diffCount).headOption match {
            // Should not happen, types are lying...
            case None =>
              IrNodesNotEqual(s"Could not find a match for node", e.path)

            case Some(d) =>
              d
          }

        case a :: as =>
          e.isEqualTo(a, strict, rules, bePermissive) match {
            case success @ IrNodesEqual =>
              success

            case failure @ IrNodesNotEqual(_) =>
              rec(e, as, failure :: fails)
          }
      }

    rec(expected, possibleMatches, Nil)
  }

  private def permissiveCheckChildren(strict: Boolean,
                                      bePermissive: Boolean,
                                      rules: IrNodeMatchingRules,
                                      a: List[IrNode],
                                      b: List[IrNode]): IrNodeEqualityResult =
    a.map { actual =>
      findClosestMatch(actual, b, strict, rules, bePermissive)
    }

  val childrenTest: (Boolean, IrNodePath, Boolean, Boolean, IrNodeMatchingRules, IrNode, IrNode) => (
      List[IrNode],
      List[IrNode]
  ) => IrNodeEqualityResult =
    (strict, path, isXml, bePermissive, rules, parentA, parentB) =>
      (a, b) => {
        if (strict) {
          if (a.length < b.length && (!bePermissive || (bePermissive && parentA.isArray && !isXml))) {
            val parentCheck: Option[IrNodeEqualityResult] =
              RuleChecks.checkForNode(
                rules.findMinArrayLengthRule(path, parentA.isXml),
                path,
                parentA,
                parentB
              )

            val childrenCheck: IrNodeEqualityResult = parentCheck match {
              case Some(pc) if pc.isEqual =>
                // If there was a min array length rule (and we know the arrays are unbalanced)
                // then we need to populate an 'A' array to compare with 'B' so that we check
                // everything correctly, running on the assumption that at this point, the user
                // is relying on rules to validate because frankly all bets for normal comparison
                // are off!
                val newA = b.map(_ => a.headOption).collect { case Some(s) => s }

                strictCheckChildren(path, strict, bePermissive, rules, newA, b, ignoreLength = false)

              case _ =>
                RuleChecks
                  .checkForNode(rules, path, parentA, parentB)
                  .getOrElse {
                    strictCheckChildren(path, strict, bePermissive, rules, a, b, ignoreLength = false)
                  }
            }

            parentCheck.map(p => p + childrenCheck).getOrElse(childrenCheck)

          } else if (a.length < b.length && bePermissive && isXml) {
            val newA = b.map(_ => a.headOption).collect { case Some(s) => s }

            val maybeResult: Option[IrNodeEqualityResult] = newA
              .zip(b)
              .map { p =>
                RuleChecks.checkForNode(rules, p._2.path, p._1, p._2)
              }
              .collect {
                case Some(d @ IrNodesNotEqual(_)) => d
              } match {
              case Nil =>
                None

              case x :: xs =>
                Option(xs.foldLeft[IrNodeEqualityResult](x)(_ + _))
            }

            val equalityResult = strictCheckChildren(path, strict, bePermissive, rules, a, b, ignoreLength = true)

            maybeResult.getOrElse(equalityResult)

          } else if (parentA.isArray && a.length == b.length) {
            strictCheckChildren(path, strict, bePermissive, rules, a, b, ignoreLength = false)
          } else if (isXml) {
            strictCheckChildren(path, strict, bePermissive, rules, a, b, ignoreLength = false)
          } else {
            permissiveCheckChildren(strict, bePermissive, rules, a, b)
          }
        } else {
          val newA = b.map(_ => a.headOption).collect { case Some(s) => s }

          val maybeResult: Option[IrNodeEqualityResult] = newA
            .zip(b)
            .map { p =>
              RuleChecks.checkForNode(rules, p._2.path, p._1, p._2)
            }
            .collect {
              case Some(d @ IrNodesNotEqual(_)) => d
            } match {
            case Nil =>
              None

            case x :: xs =>
              Option(xs.foldLeft[IrNodeEqualityResult](x)(_ + _))
          }

          val equalityResult = permissiveCheckChildren(strict, bePermissive, rules, a, b)

          maybeResult.getOrElse(equalityResult)
        }
    }

  private val checkAttributesTest
    : (IrNodePath, Boolean, IrNodeMatchingRules) => (IrNodeAttributes, IrNodeAttributes) => IrNodeEqualityResult =
    (path, isXml, rules) =>
      (a, b) =>
        a.attributes.toList.map { p =>
          b.attributes.get(p._1) match {
            case None =>
              IrNodesNotEqual(s"Attribute ${p._1} was missing", path)

            case Some(v: IrNodeAttribute) =>
              if (v == p._2) IrNodesEqual
              else {
                RuleChecks
                  .checkForPrimitive(
                    rules,
                    p._2.path,
                    Option(p._2.value),
                    Option(v.value),
                    checkParentTypeRule = true,
                    isXml
                  )
                  .getOrElse(
                    IrNodesNotEqual(
                      s"Attribute value for '${p._1}' of '${p._2.value.renderAsString}' does not equal '${v.value.renderAsString}'",
                      path
                    )
                  )
              }
          }
    }

  val attributesTest: (Boolean, Boolean, Boolean, IrNodePath, IrNodeMatchingRules) => (
      IrNodeAttributes,
      IrNodeAttributes
  ) => IrNodeEqualityResult =
    (strict, isXml, bePermissive, path, rules) =>
      (a, b) =>
        if (strict) {
          val as      = a.attributes.toList
          val bs      = b.attributes.toList
          val asNames = as.map(_._1)
          val bsNames = bs.map(_._1)

          if (asNames.length != bsNames.length && !bePermissive) {
            IrNodesNotEqual(
              s"Differing number of attributes between ['${asNames.mkString(", ")}'] and ['${bsNames.mkString(", ")}']",
              path
            )
          } else {
            checkAttributesTest(path, isXml, rules)(a, b)
          }

        } else checkAttributesTest(path, isXml, rules)(a, b)

  def check[A](f: (A, A) => IrNodeEqualityResult, propA: A, propB: A): IrNodeEqualityResult = f(propA, propB)

}

sealed trait IrNodeEqualityResult {

  val isEqual: Boolean

  def +(other: IrNodeEqualityResult): IrNodeEqualityResult =
    (this, other) match {
      case (IrNodesEqual, IrNodesEqual)               => IrNodesEqual
      case (IrNodesEqual, r @ IrNodesNotEqual(_))     => r
      case (l @ IrNodesNotEqual(_), IrNodesEqual)     => l
      case (IrNodesNotEqual(d1), IrNodesNotEqual(d2)) => IrNodesNotEqual(d1 ++ d2)
    }

  def renderAsString: String =
    this match {
      case IrNodesEqual =>
        "Nodes equal"

      case n: IrNodesNotEqual =>
        n.renderDifferences
    }

}
case object IrNodesEqual extends IrNodeEqualityResult {
  val isEqual: Boolean = true
}
case class IrNodesNotEqual(differences: List[IrNodeDiff]) extends IrNodeEqualityResult {
  val isEqual: Boolean = false
  val diffCount: Int   = differences.length

  def renderDifferencesListWithRules(rules: IrNodeMatchingRules, isXml: Boolean): List[String] =
    differences
      .groupBy(_.path.renderAsString)
      .map { k =>
        val path = k._2.headOption.map(_.path).getOrElse(IrNodePath.empty)
        val relevantRules = IrNodeMatchingRules(rules.findForPath(path, isXml), RuleProcessTracing.disabled) + rules
          .findAncestralTypeRule(path, isXml)

        s"""Node at: ${k._1}
         |  ${k._2.map(_.message).mkString("\n  ")}
         |
         |> Rules:
         |${relevantRules.renderAsString}
         |
       """.stripMargin
      }
      .toList

  def renderDifferencesList: List[String] =
    differences
      .groupBy(_.path.renderAsString)
      .map { k =>
        s"""Node at: ${k._1}
         |  ${k._2.map(_.message).mkString("\n  ")}
       """.stripMargin
      }
      .toList

  def renderDifferences: String =
    renderDifferencesList.mkString("\n")
}

case class IrNodeDiff(message: String, path: IrNodePath)

object IrNodesNotEqual {
  def apply(message: String, path: IrNodePath): IrNodesNotEqual = IrNodesNotEqual(List(IrNodeDiff(message, path)))
}

object IrNode {

  def empty: IrNode =
    IrNode("", None, Nil, None, IrNodeAttributes.empty, isArray = false, isXml = false, IrNodePathEmpty)

  def apply(label: String): IrNode =
    IrNode(label, None, Nil, None, IrNodeAttributes.empty, isArray = false, isXml = false, IrNodePathEmpty)

  def apply(label: String, value: IrNodePrimitive): IrNode =
    IrNode(label, Option(value), Nil, None, IrNodeAttributes.empty, isArray = false, isXml = false, IrNodePathEmpty)

  def apply(label: String, value: Option[IrNodePrimitive]): IrNode =
    IrNode(label, value, Nil, None, IrNodeAttributes.empty, isArray = false, isXml = false, IrNodePathEmpty)

  def apply(label: String, children: IrNode*): IrNode =
    IrNode(label, None, children.toList, None, IrNodeAttributes.empty, isArray = false, isXml = false, IrNodePathEmpty)

  def apply(label: String, children: List[IrNode]): IrNode =
    IrNode(label, None, children, None, IrNodeAttributes.empty, isArray = false, isXml = false, IrNodePathEmpty)

}

sealed trait IrNodePrimitive {
  def isString: Boolean
  def isNumber: Boolean
  def isBoolean: Boolean
  def isNull: Boolean
  def asString: Option[String]
  def asNumber: Option[Double]
  def asBoolean: Option[Boolean]
  def renderAsString: String
  def primitiveTypeName: String
}
case class IrStringNode(value: String) extends IrNodePrimitive {
  def isString: Boolean          = true
  def isNumber: Boolean          = false
  def isBoolean: Boolean         = false
  def isNull: Boolean            = false
  def asString: Option[String]   = Option(value)
  def asNumber: Option[Double]   = None
  def asBoolean: Option[Boolean] = None
  def renderAsString: String     = value
  def primitiveTypeName: String  = "string"
}
case class IrNumberNode(value: Double) extends IrNodePrimitive {
  def isString: Boolean          = false
  def isNumber: Boolean          = true
  def isBoolean: Boolean         = false
  def isNull: Boolean            = false
  def asString: Option[String]   = None
  def asNumber: Option[Double]   = Option(value)
  def asBoolean: Option[Boolean] = None
  def renderAsString: String     = value.toString.replaceAll("\\.0", "")
  def primitiveTypeName: String  = "number"
}
case class IrBooleanNode(value: Boolean) extends IrNodePrimitive {
  def isString: Boolean          = false
  def isNumber: Boolean          = false
  def isBoolean: Boolean         = true
  def isNull: Boolean            = false
  def asString: Option[String]   = None
  def asNumber: Option[Double]   = None
  def asBoolean: Option[Boolean] = Option(value)
  def renderAsString: String     = value.toString
  def primitiveTypeName: String  = "boolean"
}
case object IrNullNode extends IrNodePrimitive {
  def isString: Boolean          = false
  def isNumber: Boolean          = false
  def isBoolean: Boolean         = false
  def isNull: Boolean            = true
  def asString: Option[String]   = None
  def asNumber: Option[Double]   = None
  def asBoolean: Option[Boolean] = None
  def renderAsString: String     = "null"
  def primitiveTypeName: String  = "null"
}

object IrNodeAttributes {

  def empty: IrNodeAttributes = IrNodeAttributes(Map.empty[String, IrNodeAttribute])

}

case class IrNodeAttributes(attributes: Map[String, IrNodeAttribute]) {

  def +(other: IrNodeAttributes): IrNodeAttributes =
    IrNodeAttributes(this.attributes ++ other.attributes)

}
case class IrNodeAttribute(value: IrNodePrimitive, path: IrNodePath)

object RuleChecks {

  implicit private def resultToOption(v: IrNodeEqualityResult): Option[IrNodeEqualityResult] =
    Option(v)

  implicit private def listResultsToOption(l: List[IrNodeEqualityResult]): Option[IrNodeEqualityResult] =
    l match {
      case Nil =>
        None

      case x :: xs =>
        xs.foldLeft(x)(_ + _)
    }

  def checkForNode(rules: IrNodeMatchingRules,
                   path: IrNodePath,
                   expected: IrNode,
                   actual: IrNode): Option[IrNodeEqualityResult] =
    rules.validateNode(path, expected, actual)

  def checkForPrimitive(rules: IrNodeMatchingRules,
                        path: IrNodePath,
                        expected: Option[IrNodePrimitive],
                        actual: Option[IrNodePrimitive],
                        checkParentTypeRule: Boolean,
                        isXml: Boolean): Option[IrNodeEqualityResult] =
    (expected, actual) match {
      case (Some(e), Some(a)) =>
        rules.validatePrimitive(path, e, a, checkParentTypeRule, isXml)

      case (Some(e), None) =>
        IrNodesNotEqual(s"Missing 'actual' value '${e.renderAsString}'", path)

      case (None, Some(a)) =>
        IrNodesNotEqual(s"Missing 'expected' value '${a.renderAsString}'", path)

      case (None, None) =>
        IrNodesEqual
    }

}

sealed trait IrNodeMatchPermissivity {
  val bePermissive: Boolean
}
case object NonPermissive extends IrNodeMatchPermissivity {
  val bePermissive: Boolean = false
}
case object Permissive extends IrNodeMatchPermissivity {
  val bePermissive: Boolean = true
}

object IrNodeMatchPermissivity {
  implicit val defaultPermissivity: IrNodeMatchPermissivity = NonPermissive
}

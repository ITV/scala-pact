package com.itv.scalapact.shared.matchir

import com.itv.scalapact.shared.matchir.IrNodeEqualityResult.{IrNodesEqual, IrNodesNotEqual}
import com.itv.scalapact.shared.matchir.IrNodePath.IrNodePathEmpty

final case class IrNode(label: String,
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
final case class IrStringNode(value: String) extends IrNodePrimitive {
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
final case class IrNumberNode(value: Double) extends IrNodePrimitive {
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
final case class IrBooleanNode(value: Boolean) extends IrNodePrimitive {
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

final case class IrNodeAttributes(attributes: Map[String, IrNodeAttribute]) {

  def +(other: IrNodeAttributes): IrNodeAttributes =
    IrNodeAttributes(this.attributes ++ other.attributes)

}
final case class IrNodeAttribute(value: IrNodePrimitive, path: IrNodePath)

object RuleChecks {
  private def foldResults(l: List[IrNodeEqualityResult]): Option[IrNodeEqualityResult] =
    l match {
      case Nil =>
        None

      case x :: xs =>
        Some(xs.foldLeft(x)(_ + _))
    }

  def checkForNode(rules: IrNodeMatchingRules,
                   path: IrNodePath,
                   expected: IrNode,
                   actual: IrNode): Option[IrNodeEqualityResult] =
    foldResults(rules.validateNode(path, expected, actual))

  def checkForPrimitive(rules: IrNodeMatchingRules,
                        path: IrNodePath,
                        expected: Option[IrNodePrimitive],
                        actual: Option[IrNodePrimitive],
                        checkParentTypeRule: Boolean,
                        isXml: Boolean): Option[IrNodeEqualityResult] =
    (expected, actual) match {
      case (Some(e), Some(a)) =>
        foldResults(rules.validatePrimitive(path, e, a, checkParentTypeRule, isXml))

      case (Some(e), None) =>
        Some(IrNodesNotEqual(s"Missing 'actual' value '${e.renderAsString}'", path))

      case (None, Some(a)) =>
        Some(IrNodesNotEqual(s"Missing 'expected' value '${a.renderAsString}'", path))

      case (None, None) =>
        Some(IrNodesEqual)
    }

}

sealed abstract class IrNodeMatchPermissivity(val bePermissive: Boolean)

object IrNodeMatchPermissivity {
  implicit val defaultPermissivity: IrNodeMatchPermissivity = NonPermissive

  case object NonPermissive extends IrNodeMatchPermissivity(false)
  case object Permissive extends IrNodeMatchPermissivity(true)
}

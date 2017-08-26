package com.itv.scalapactcore.common.matchir

case class IrNode(label: String, value: Option[IrNodePrimitive], children: List[IrNode], ns: Option[String], attributes: IrNodeAttributes, path: IrNodePath) {

  import IrNodeEqualityResult._

  def =~(other: IrNode): IrNodeEqualityResult = isEqualTo(other, strict = false)
  def =<>=(other: IrNode): IrNodeEqualityResult = isEqualTo(other, strict = true)

  def isEqualTo(other: IrNode, strict: Boolean): IrNodeEqualityResult = {
    check[String](labelTest(path), this.label, other.label) +
    check[Option[IrNodePrimitive]](valueTest(strict)(path), this.value, other.value) +
    check[List[IrNode]](childrenTest(strict)(path), this.children, other.children) +
    check[Option[String]](namespaceTest(path), this.ns, other.ns) +
    check[IrNodeAttributes](attributesTest(strict)(path), this.attributes, other.attributes) +
    check[IrNodePath](pathTest(strict)(path), this.path, other.path)
  }

  val arrays: Map[String, List[IrNode]] =
    children.groupBy(_.label).filter(_._2.length > 1)

  val arraysKeys: List[String] = arrays.keys.toList

  def withNamespace(ns: String): IrNode = this.copy(ns = Option(ns))
  def withAttributes(attributes: IrNodeAttributes): IrNode = this.copy(attributes = attributes)
  def withPath(path: IrNodePath): IrNode = this.copy(path = path)

  def renderAsString: String = renderAsString(0)

  def renderAsString(indent: Int): String = {
    val i = List.fill(indent)("  ").mkString
    val n = ns.map("  namespace: " + _ + "").getOrElse("")
    val v = value.map(v => "  value: " + v.renderAsString).getOrElse("")
    val a = if(attributes.attributes.isEmpty) "" else s"  attributes: [${attributes.attributes.map(p => p._1 + "=" + p._2.value.renderAsString).mkString(", ")}]"
    val l = if(arraysKeys.isEmpty) "" else s"  arrays: [${arraysKeys.mkString(", ")}]"
    val c = if(children.isEmpty) "" else "\n" + children.map(_.renderAsString(indent + 1)).mkString("\n")
    val p = "  " + path.renderAsString
    s"$i- $label$n$v$a$l$p$c"
  }

}

object IrNodeEqualityResult {

  val labelTest: IrNodePath => (String, String) => IrNodeEqualityResult =
    path => (a, b) => {
      if(a == b) IrNodesEqual else IrNodesNotEqual(s"Label '$a' did not match '$b'", path)
    }

  val valueTest: Boolean => IrNodePath => (Option[IrNodePrimitive], Option[IrNodePrimitive]) => IrNodeEqualityResult =
    strict => path =>
      if(strict) {
        case (Some(v1: IrNodePrimitive), Some(v2: IrNodePrimitive)) =>
          if(v1 == v2) IrNodesEqual else IrNodesNotEqual(s"Value '${v1.renderAsString}' did not match '${v2.renderAsString}'", path)

        case (Some(v1: IrNodePrimitive), None) =>
          IrNodesNotEqual(s"Value '${v1.renderAsString}' did not match empty value", path)

        case (None, Some(v2: IrNodePrimitive)) =>
          IrNodesNotEqual(s"Empty value did not match '${v2.renderAsString}'", path)

        case (None, None) =>
          IrNodesEqual
      } else {
        case (Some(v1: IrNodePrimitive), Some(v2: IrNodePrimitive)) =>
          if(v1 == v2) IrNodesEqual else IrNodesNotEqual(s"Value '${v1.renderAsString}' did not match '${v2.renderAsString}'", path)

        case (Some(v1: IrNodePrimitive), None) =>
          IrNodesNotEqual(s"Value '${v1.renderAsString}' did not match empty value", path)

        case (None, Some(v2: IrNodePrimitive)) =>
          IrNodesEqual

        case (None, None) =>
          IrNodesEqual
      }

  val namespaceTest: IrNodePath => (Option[String], Option[String]) => IrNodeEqualityResult = path => {
    case (Some(v1: String), Some(v2: String)) =>
      if(v1 == v2) IrNodesEqual else IrNodesNotEqual(s"Namespace '$v1' did not match '$v2'", path)

    case (Some(v1: String), None) =>
      IrNodesNotEqual(s"Namespace '$v1' did not match empty namespace", path)

    case (None, Some(v2: String)) =>
      IrNodesNotEqual(s"Empty namespace did not match '$v2'", path)

    case (None, None) =>
      IrNodesEqual
  }

  val pathTest: Boolean => IrNodePath => (IrNodePath, IrNodePath) => IrNodeEqualityResult =
    strict => path => (a, b) =>
      if(strict)
        if(a === b) IrNodesEqual else IrNodesNotEqual(s"Path '${a.renderAsString}' does not equal '${b.renderAsString}'", path)
      else
        if(a =~= b) IrNodesEqual else IrNodesNotEqual(s"Path '${a.renderAsString}' does not equal '${b.renderAsString}'", path)

  implicit private def listOfResultsToResult(l: List[IrNodeEqualityResult]): IrNodeEqualityResult =
    l match {
      case Nil => IrNodesEqual
      case x :: xs => xs.foldLeft(x)(_ + _)
    }

  val childrenTest: Boolean => IrNodePath => (List[IrNode], List[IrNode]) => IrNodeEqualityResult =
    strict => path => (a, b) =>
      if(strict) {
        if(a.length != b.length) {
          IrNodesNotEqual(s"Differing number of children. Expected ${a.length} got ${b.length}", path)
        } else {
          a.zip(b).map(p => p._1.isEqualTo(p._2, strict))
        }
      } else {
        a.map { n1 =>
          b.find(n2 => n1.isEqualTo(n2, strict).isEqual) match {
            case Some(_) => IrNodesEqual
            case None => IrNodesNotEqual(s"Could not find match for:\n${n1.renderAsString}", path)
          }
        }
      }

  private val checkAttributesTest: IrNodePath => (IrNodeAttributes, IrNodeAttributes) => IrNodeEqualityResult = path => (a, b) =>
    a.attributes.toList.map { p =>
      b.attributes.get(p._1) match {
        case None =>
          IrNodesNotEqual(s"Attribute ${p._1} was missing", path)

        case Some(v: IrNodeAttribute) =>
          if(v == p._2) IrNodesEqual else IrNodesNotEqual(s"Attribute value for '${p._1}' of '${p._2.value.renderAsString}' does not equal '${v.value.renderAsString}'", path)
      }
    }


  val attributesTest: Boolean => IrNodePath => (IrNodeAttributes, IrNodeAttributes) => IrNodeEqualityResult =
    strict => path => (a, b) =>
      if(strict) {
        val as = a.attributes.toList
        val bs = b.attributes.toList
        val asNames = as.map(_._1)
        val bsNames = bs.map(_._1)

        if(asNames.length != bsNames.length) {
          IrNodesNotEqual(s"Differing number of attributes between ['${asNames.mkString(", ")}'] and ['${bsNames.mkString(", ")}']", path)
        } else if(asNames != bsNames) {
          IrNodesNotEqual(s"Differing attribute order between ['${asNames.mkString(", ")}'] and ['${bsNames.mkString(", ")}']", path)
        } else {
          checkAttributesTest(path)(a, b)
        }

      } else checkAttributesTest(path)(a, b)

  def check[A](f: (A, A) => IrNodeEqualityResult, propA: A, propB: A): IrNodeEqualityResult = f(propA, propB)

}

sealed trait IrNodeEqualityResult {

  val isEqual: Boolean

  def +(other: IrNodeEqualityResult): IrNodeEqualityResult =
    (this, other) match {
      case (IrNodesEqual, IrNodesEqual) => IrNodesEqual
      case (IrNodesEqual, r @ IrNodesNotEqual(_)) => r
      case (l @ IrNodesNotEqual(_), IrNodesEqual) => l
      case (IrNodesNotEqual(d1), IrNodesNotEqual(d2)) => IrNodesNotEqual(d1 ++ d2)
    }

}
case object IrNodesEqual extends IrNodeEqualityResult {
  val isEqual: Boolean = true
}
case class IrNodesNotEqual(differences: List[IrNodeDiff]) extends IrNodeEqualityResult {
  val isEqual: Boolean = false

  def renderDifferences: String =
    differences.map(d => s"""Node at: ${d.path.renderAsString}\n  ${d.message}""").mkString("\n")
}

case class IrNodeDiff(message: String, path: IrNodePath)

object IrNodesNotEqual {
  def apply(message: String, path: IrNodePath): IrNodesNotEqual = IrNodesNotEqual(List(IrNodeDiff(message, path)))
}

object IrNode {

  def empty: IrNode =
    IrNode("", None, Nil, None, IrNodeAttributes.empty, IrNodePathEmpty)

  def apply(label: String): IrNode =
    IrNode(label, None, Nil, None, IrNodeAttributes.empty, IrNodePathEmpty)

  def apply(label: String, value: IrNodePrimitive): IrNode =
    IrNode(label, Option(value), Nil, None, IrNodeAttributes.empty, IrNodePathEmpty)

  def apply(label: String, value: Option[IrNodePrimitive]): IrNode =
    IrNode(label, value, Nil, None, IrNodeAttributes.empty, IrNodePathEmpty)

  def apply(label: String, children: IrNode*): IrNode =
    IrNode(label, None, children.toList, None, IrNodeAttributes.empty, IrNodePathEmpty)

  def apply(label: String, children: List[IrNode]): IrNode =
    IrNode(label, None, children, None, IrNodeAttributes.empty, IrNodePathEmpty)

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
  def isString: Boolean = true
  def isNumber: Boolean = false
  def isBoolean: Boolean = false
  def isNull: Boolean = false
  def asString: Option[String] = Option(value)
  def asNumber: Option[Double] = None
  def asBoolean: Option[Boolean] = None
  def renderAsString: String = value
  def primitiveTypeName: String = "string"
}
case class IrNumberNode(value: Double) extends IrNodePrimitive {
  def isString: Boolean = false
  def isNumber: Boolean = true
  def isBoolean: Boolean = false
  def isNull: Boolean = false
  def asString: Option[String] = None
  def asNumber: Option[Double] = Option(value)
  def asBoolean: Option[Boolean] = None
  def renderAsString: String = value.toString
  def primitiveTypeName: String = "number"
}
case class IrBooleanNode(value: Boolean) extends IrNodePrimitive {
  def isString: Boolean = false
  def isNumber: Boolean = false
  def isBoolean: Boolean = true
  def isNull: Boolean = false
  def asString: Option[String] = None
  def asNumber: Option[Double] = None
  def asBoolean: Option[Boolean] = Option(value)
  def renderAsString: String = value.toString
  def primitiveTypeName: String = "boolean"
}
case object IrNullNode extends IrNodePrimitive {
  def isString: Boolean = false
  def isNumber: Boolean = false
  def isBoolean: Boolean = false
  def isNull: Boolean = true
  def asString: Option[String] = None
  def asNumber: Option[Double] = None
  def asBoolean: Option[Boolean] = None
  def renderAsString: String = "null"
  def primitiveTypeName: String = "null"
}

object IrNodeAttributes {

  def empty: IrNodeAttributes = IrNodeAttributes(Map.empty[String, IrNodeAttribute])

}

case class IrNodeAttributes(attributes: Map[String, IrNodeAttribute]) {

  def +(other: IrNodeAttributes): IrNodeAttributes =
    IrNodeAttributes(this.attributes ++ other.attributes)

}
case class IrNodeAttribute(value: IrNodePrimitive, path: IrNodePath)
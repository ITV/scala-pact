package com.itv.scalapactcore.common.matchir

case class IrNode(label: String, value: Option[IrNodePrimitive], children: List[IrNode], ns: Option[String], attributes: Map[String, IrNodePrimitive], path: IrNodePath) {

  import IrNodeEqualityResult._

  def =~=(other: IrNode): IrNodeEqualityResult = {
    check[String](labelTest(path), this.label, other.label) +
    check[Option[IrNodePrimitive]](valueTest(path), this.value, other.value) +
    check[List[IrNode]](childrenTest(path), this.children, other.children) +
    check[Option[String]](namespaceTest(path), this.ns, other.ns) +
    check[Map[String, IrNodePrimitive]](attributesTest(path), this.attributes, other.attributes) +
    check[IrNodePath](pathTest(path), this.path, other.path)
  }

  val arrays: Map[String, List[IrNode]] =
    children.groupBy(_.label).filter(_._2.length > 1)

  val arraysKeys: List[String] = arrays.keys.toList

  def withNamespace(ns: String): IrNode = this.copy(ns = Option(ns))
  def withAttributes(attributes: Map[String, IrNodePrimitive]): IrNode = this.copy(attributes = attributes)

  def renderAsString: String = renderAsString(0)

  def renderAsString(indent: Int): String = {
    val i = List.fill(indent)("  ").mkString
    val n = ns.map("  namespace: " + _ + "").getOrElse("")
    val v = value.map(v => "  value: " + v.renderAsString).getOrElse("")
    val a = if(attributes.isEmpty) "" else s"  attributes: [${attributes.map(p => p._1 + "=" + p._2.renderAsString).mkString(", ")}]"
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

  val valueTest: IrNodePath => (Option[IrNodePrimitive], Option[IrNodePrimitive]) => IrNodeEqualityResult = path => {
    case (Some(v1: IrNodePrimitive), Some(v2: IrNodePrimitive)) =>
      if(v1 == v2) IrNodesEqual else IrNodesNotEqual(s"Value '${v1.renderAsString}' did not match '${v2.renderAsString}'", path)

    case (Some(v1: IrNodePrimitive), None) =>
      IrNodesNotEqual(s"Value '${v1.renderAsString}' did not match empty value", path)

    case (None, Some(v2: IrNodePrimitive)) =>
      IrNodesNotEqual(s"Empty value did not match '${v2.renderAsString}'", path)

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

  val pathTest: IrNodePath => (IrNodePath, IrNodePath) => IrNodeEqualityResult =
    path => (a, b) =>
      if(a === b) IrNodesEqual else IrNodesNotEqual(s"Path '${a.renderAsString}' does not equal '${b.renderAsString}'", path)

  val childrenTest: IrNodePath => (List[IrNode], List[IrNode]) => IrNodeEqualityResult = ???

  val attributesTest: IrNodePath => (Map[String, IrNodePrimitive], Map[String, IrNodePrimitive]) => IrNodeEqualityResult = ???

  def check[A](f: (A, A) => IrNodeEqualityResult, propA: A, propB: A): IrNodeEqualityResult = f(propA, propB)

}

sealed trait IrNodeEqualityResult {

  def +(other: IrNodeEqualityResult): IrNodeEqualityResult =
    (this, other) match {
      case (IrNodesEqual, IrNodesEqual) => IrNodesEqual
      case (IrNodesEqual, r @ IrNodesNotEqual(_)) => r
      case (l @ IrNodesNotEqual(_), IrNodesEqual) => l
      case (IrNodesNotEqual(d1), IrNodesNotEqual(d2)) => IrNodesNotEqual(d1 ++ d2)
    }

}
case object IrNodesEqual extends IrNodeEqualityResult
case class IrNodesNotEqual(differences: List[IrNodeDiff]) extends IrNodeEqualityResult

case class IrNodeDiff(message: String, path: IrNodePath)

object IrNodesNotEqual {
  def apply(message: String, path: IrNodePath): IrNodesNotEqual = IrNodesNotEqual(List(IrNodeDiff(message, path)))
}

object IrNode {

  def apply(label: String): IrNode =
    IrNode(label, None, Nil, None, Map.empty[String, IrNodePrimitive], IrNodePathEmpty)

  def apply(label: String, value: IrNodePrimitive): IrNode =
    IrNode(label, Option(value), Nil, None, Map.empty[String, IrNodePrimitive], IrNodePathEmpty)

  def apply(label: String, value: Option[IrNodePrimitive]): IrNode =
    IrNode(label, value, Nil, None, Map.empty[String, IrNodePrimitive], IrNodePathEmpty)

  def apply(label: String, children: IrNode*): IrNode =
    IrNode(label, None, children.toList, None, Map.empty[String, IrNodePrimitive], IrNodePathEmpty)

  def apply(label: String, children: List[IrNode]): IrNode =
    IrNode(label, None, children, None, Map.empty[String, IrNodePrimitive], IrNodePathEmpty)

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
}

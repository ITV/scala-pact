package com.itv.scalapactcore.common.matchir

case class IrNode(label: String, value: Option[IrNodePrimitive], children: List[IrNode], ns: Option[String], attributes: Map[String, IrNodePrimitive]) {

  val arrays: Map[String, List[IrNode]] =
    children.groupBy(_.label).filter(_._2.length > 1)

  val arraysKeys: List[String] = arrays.keys.toList

  def withNamespace(ns: String): IrNode = this.copy(ns = Option(ns))
  def withAttributes(attributes: Map[String, IrNodePrimitive]): IrNode = this.copy(attributes = attributes)

  def renderAsString: String = renderAsString(0)

  def renderAsString(indent: Int = 0): String = {
    val i = List.fill(indent)("  ").mkString
    val n = ns.map("  namespace: " + _ + "").getOrElse("")
    val v = value.map(v => "  value: " + v.renderAsString).getOrElse("")
    val a = if(attributes.isEmpty) "" else s"  atrributes: [${attributes.map(p => p._1 + "=" + p._2.renderAsString).mkString(", ")}]"
    val l = if(arraysKeys.isEmpty) "" else s"  arrays: [${arraysKeys.mkString(", ")}]"
    val c = if(children.isEmpty) "" else "\n" + children.map(_.renderAsString(indent + 1)).mkString("\n")
    s"$i- $label$n$v$a$l$c"
  }

}

object IrNode {

  def apply(label: String): IrNode =
    IrNode(label, None, Nil, None, Map.empty[String, IrNodePrimitive])

  def apply(label: String, value: IrNodePrimitive): IrNode =
    IrNode(label, Option(value), Nil, None, Map.empty[String, IrNodePrimitive])

  def apply(label: String, value: Option[IrNodePrimitive]): IrNode =
    IrNode(label, value, Nil, None, Map.empty[String, IrNodePrimitive])

  def apply(label: String, children: IrNode*): IrNode =
    IrNode(label, None, children.toList, None, Map.empty[String, IrNodePrimitive])

  def apply(label: String, children: List[IrNode]): IrNode =
    IrNode(label, None, children, None, Map.empty[String, IrNodePrimitive])

}

case class IrArrayNode()

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

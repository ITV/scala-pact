package com.itv.scalapact.shared.matchir

import com.itv.scalapact.shared.matchir.IrNodePath.IrNodePathEmpty
import com.itv.scalapact.shared.utils.Helpers.{isBooleanValueRegex, isNumericValueRegex, safeStringToBoolean, safeStringToDouble}

import scala.util.Try
import scala.xml.{Elem, Node, XML}

object MatchIr {
  def fromXmlString(xmlString: String): Option[IrNode] =
    Try(XML.loadString(xmlString)).toOption.map(fromXml)

  def fromXml(xml: Elem): IrNode =
    nodeToIrNode(scala.xml.Utility.trim(xml), IrNodePathEmpty)

  def fromJSON(fromJson: String => Option[IrNode])(jsonString: String): Option[IrNode] =
    fromJson(jsonString)

  private def convertAttributes(attributes: Map[String, String], pathToParent: IrNodePath): IrNodeAttributes =
    attributes.toList.reverse
      .map {
        case (k, v) if v == null =>
          IrNodeAttributes(Map(k -> IrNodeAttribute(IrNullNode, pathToParent <@ k)))

        case (k, v) if v.matches(isNumericValueRegex) =>
          safeStringToDouble(v)
            .map(IrNumberNode)
            .map(vv => IrNodeAttributes(Map(k -> IrNodeAttribute(vv, pathToParent <@ k))))
            .getOrElse(IrNodeAttributes.empty)

        case (k, v) if v.matches(isBooleanValueRegex) =>
          safeStringToBoolean(v)
            .map(IrBooleanNode)
            .map(vv => IrNodeAttributes(Map(k -> IrNodeAttribute(vv, pathToParent <@ k))))
            .getOrElse(IrNodeAttributes.empty)

        case (k, v) =>
          IrNodeAttributes(Map(k -> IrNodeAttribute(IrStringNode(v), pathToParent <@ k)))

      }
      .foldLeft(IrNodeAttributes.empty)(_ + _)

  private def childNodesToValueMaybePrimitive(nodes: List[Node], value: String): Option[IrNodePrimitive] =
    nodes match {
      case Nil if value == null                      => Option(IrNullNode)
      case Nil if value.isEmpty                      => None
      case Nil if value.matches(isNumericValueRegex) => safeStringToDouble(value).map(IrNumberNode)
      case Nil if value.matches(isBooleanValueRegex) => safeStringToBoolean(value).map(IrBooleanNode)
      case Nil                                       => Option(IrStringNode(value))
      case _                                         => None
    }

  private def extractNodeValue(node: Node): Option[IrNodePrimitive] =
    childNodesToValueMaybePrimitive(node.child.flatMap(_.child).toList, node.child.text)

  private def extractNodeChildren(node: Node, pathToParent: IrNodePath): List[IrNode] =
    node.child.toList.map(n => nodeToIrNode(n, pathToParent))

  private def nodeToIrNode(node: Node, initialPath: IrNodePath): IrNode =
    extractNodeValue(node) match {
      case nodeValue: Some[IrNodePrimitive] =>
        IrNode(node.label,
               nodeValue,
               Nil,
               Option(node.prefix),
               convertAttributes(node.attributes.asAttrMap, initialPath <~ node.label),
               isArray = false,
               isXml = true,
               initialPath <~ node.label)

      case None =>
        val children = extractNodeChildren(node, initialPath <~ node.label)
        val isArray: Boolean =
          children match {
            case Nil =>
              false

            case x :: _ if children.length > 1 && children.forall(_.label == x.label) =>
              true

            case _ =>
              false
          }

        val updatedChildren =
          if (isArray) children.zipWithIndex.map(ci => ci._1.withPath(ci._1.path <~ ci._2))
          else children

        IrNode(
          node.label,
          None,
          updatedChildren,
          Option(node.prefix),
          convertAttributes(node.attributes.asAttrMap, initialPath <~ node.label),
          isArray,
          isXml = true,
          initialPath <~ node.label
        )
    }

}

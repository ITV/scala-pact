package com.itv.scalapact.argonaut62

import argonaut._
import com.itv.scalapact.shared.ColourOuput._
import com.itv.scalapact.shared.{IJsonConversionFunctions, PactLogger}
import com.itv.scalapact.shared.matchir.MatchIrConstants.{rootNodeLabel, unnamedNodeLabel}
import com.itv.scalapact.shared.matchir._

object JsonConversionFunctions extends IJsonConversionFunctions {

  def fromJSON(jsonString: String): Option[IrNode] =
    Parse.parseOption(jsonString).flatMap { json =>
      jsonRootToIrNode(json, IrNodePathEmpty)
    }

  def jsonToIrNode(label: String, json: Json, pathToParent: IrNodePath): IrNode = {
    def irNodeFrom(maybeNode: Option[IrNodePrimitive]) =
      IrNode(label, maybeNode).withPath(pathToParent)

    json match {
      case j: Json if j.isArray =>
        IrNode(label, jsonArrayToIrNodeList(label, j, pathToParent)).withPath(pathToParent).markAsArray

      case j: Json if j.isObject =>
        IrNode(label, jsonObjectToIrNodeList(j, pathToParent)).withPath(pathToParent)

      case j: Json if j.isNull =>
        IrNode(label, IrNullNode).withPath(pathToParent)

      case j: Json if j.isNumber && j.toString().contains(".") =>
        irNodeFrom(j.number.map(_.toBigDecimal).map(IrDecimalNode))
      case j: Json if j.isNumber =>
        irNodeFrom(j.number.flatMap(_.toBigInt).map(IrIntegerNode))

      case j: Json if j.isBool =>
        irNodeFrom(j.bool.map(IrBooleanNode))

      case j: Json if j.isString =>
        irNodeFrom(j.string.map(IrStringNode))
    }
  }

  def jsonObjectToIrNodeList(json: Json, pathToParent: IrNodePath): List[IrNode] =
    json.objectFieldsOrEmpty
      .map(l => if (l.isEmpty) unnamedNodeLabel else l)
      .map { l =>
        json.field(l).map { q =>
          jsonToIrNode(l, q, pathToParent <~ l)
        }
      }
      .collect { case Some(s) => s }

  def jsonArrayToIrNodeList(parentLabel: String, json: Json, pathToParent: IrNodePath): List[IrNode] =
    json.arrayOrEmpty.zipWithIndex
      .map(j => jsonToIrNode(parentLabel, j._1, pathToParent <~ j._2))

  def jsonRootToIrNode(json: Json, initialPath: IrNodePath): Option[IrNode] =
    json match {
      case j: Json if j.isArray =>
        Option(
          IrNode(rootNodeLabel, jsonArrayToIrNodeList(unnamedNodeLabel, j, initialPath))
            .withPath(initialPath)
            .markAsArray
        )

      case j: Json if j.isObject =>
        Option(
          IrNode(rootNodeLabel, jsonObjectToIrNodeList(j, initialPath))
            .withPath(initialPath)
        )

      case _ =>
        PactLogger.error("JSON was not an object or an array".red)
        None
    }

}

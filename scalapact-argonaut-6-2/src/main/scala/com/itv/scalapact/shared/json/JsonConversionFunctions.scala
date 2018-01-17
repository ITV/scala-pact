package com.itv.scalapact.shared.json

import argonaut._
import com.itv.scalapact.shared.IJsonConversionFunctions
import com.itv.scalapact.shared.matchir._
import com.itv.scalapact.shared.matchir.MatchIrConstants.{rootNodeLabel, unnamedNodeLabel}
import com.itv.scalapact.shared.ColourOuput._

object JsonConversionFunctions extends IJsonConversionFunctions {

  def fromJSON(jsonString: String): Option[IrNode] =
    Parse.parseOption(jsonString).flatMap { json =>
      jsonRootToIrNode(json, IrNodePathEmpty)
    }

  def jsonToIrNode(label: String, json: Json, pathToParent: IrNodePath): IrNode = {
    json match {
      case j: Json if j.isArray =>
        IrNode(label, jsonArrayToIrNodeList(label, j, pathToParent)).withPath(pathToParent)
          .markAsArray

      case j: Json if j.isObject =>
        IrNode(label, jsonObjectToIrNodeList(j, pathToParent)).withPath(pathToParent)

      case j: Json if j.isNull =>
        IrNode(label, IrNullNode).withPath(pathToParent)

      case j: Json if j.isNumber =>
        IrNode(label, j.number.flatMap(_.toDouble).map(d => IrNumberNode(d))).withPath(pathToParent)

      case j: Json if j.isBool =>
        IrNode(label, j.bool.map(IrBooleanNode)).withPath(pathToParent)

      case j: Json if j.isString =>
        IrNode(label, j.string.map(IrStringNode)).withPath(pathToParent)
    }

  }

  def jsonObjectToIrNodeList(json: Json, pathToParent: IrNodePath): List[IrNode] = {
    json
      .objectFieldsOrEmpty
      .map(l => if (l.isEmpty) unnamedNodeLabel else l)
      .map { l =>
        json.field(l).map {
          q => jsonToIrNode(l, q, pathToParent <~ l)
        }
      }
      .collect { case Some(s) => s }
  }

  def jsonArrayToIrNodeList(parentLabel: String, json: Json, pathToParent: IrNodePath): List[IrNode] = {
    json.arrayOrEmpty.zipWithIndex
      .map(j => jsonToIrNode(parentLabel, j._1, pathToParent <~ j._2))
  }

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
        println("JSON was not an object or an array".red)
        None
    }

}

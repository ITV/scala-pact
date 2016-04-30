package com.itv.scalapact.plugin.common

import scala.language.implicitConversions
import argonaut._

import scalaz._
import Scalaz._

object JsonEquality {

  implicit def toJsonEqualityWrapper(json: Json): JsonEqualityWrapper = JsonEqualityWrapper(json)

  case class JsonEqualityWrapper(json: Json) {
    def jEq(to: Json): Boolean = JsonEqualityHelper.areEqual(json, to)
  }

}

object JsonEqualityHelper {

  def areEqual(expected: Json, received: Json): Boolean = {
    expected match {
      case j: Json if j.isObject && received.isObject =>
        compareFields(expected, received, j.objectFieldsOrEmpty)

      case j: Json if j.isArray && received.isArray =>

        (j.array |@| received.array) { (ja, ra) =>
          ja.forall { jo =>
            ra.exists(ro => areEqual(jo, ro))
          }
        } match {
          case Some(matches) => matches
          case None => false
        }

      case j: Json =>
        expected == received
    }
  }

  private def compareFields(expected: Json, received: Json, expectedFields: List[Json.JsonField]): Boolean = {
    if(!expectedFields.forall(f => received.hasField(f))) false
    else {
      expectedFields.forall { field =>

        (expected.field(field) |@| received.field(field)){ areEqual(_, _) } match {
          case Some(bool) => bool
          case None => false
        }
      }
    }
  }

}

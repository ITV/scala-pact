package com.itv.scalapact.circe09

import io.circe.Json
import com.itv.scalapact.json
import org.scalatest.{FlatSpec, Matchers}
import Matchers._
import com.itv.scalapact.shared.MatchingRule
import org.scalactic.TypeCheckedTripleEquals

class IITypeInferTypeSpec extends FlatSpec with TypeCheckedTripleEquals {
  import json.ext.inferTypeInstance

  it should "infer types from a json" in {
    inferTypeInstance
      .infer(
        Json.obj(
          "key1" -> Json.fromString("value1"),
          "key2" -> Json.fromInt(333),
          "key3" -> Json.fromLong(Int.MinValue.toLong - 1),
          "key4" -> Json.fromLong(Int.MinValue.toLong + 1),
          "key5" -> Json.fromLong(Int.MaxValue.toLong + 1),
          "key6" -> Json.fromLong(Int.MaxValue.toLong - 1),
          "key7" -> Json.obj("m" -> Json.fromDouble(1.1).get),
          "key8" -> Json.arr(
            Json.fromBoolean(false),
            Json.obj(
              "key" -> Json.fromBoolean(true)
            )
          ),
          "key9" -> Json.arr(
            Json.obj(
              "key" -> Json.fromBoolean(true)
            )
          )
        )
      ) should ===(
      Map(
        ".key2"   -> "integer",
        ".key3"   -> "integer",
        ".key4"   -> "integer",
        ".key5"   -> "integer",
        ".key6"   -> "integer",
        ".key7.m" -> "decimal"
      ).mapValues(matchingRule)
    )
  }

  it should "not infer types from a json that are not objects or arrays" in {
    inferTypeInstance
      .infer(Json.fromInt(22)) should ===(Map.empty[String, MatchingRule])
  }

  it should "not infer types arrays that contain simple types" in {
    inferTypeInstance
      .infer(Json.arr(Json.fromString(""))) should ===(Map.empty[String, MatchingRule])
  }

  /*[{name: "", age: 10, nin: 1000},{name: "", age: 10}]*/
  def matchingRule(valueType: String): MatchingRule = MatchingRule(Some(valueType), None, None)

}

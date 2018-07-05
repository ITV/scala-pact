package com.itv.scalapact.circe09

import io.circe.Json
import org.scalactic.TypeCheckedTripleEquals
import org.scalatest.Matchers._
import org.scalatest.{EitherValues, FlatSpec}

class IMessageFormatSpec extends FlatSpec with EitherValues with TypeCheckedTripleEquals {
  import com.itv.scalapact.json.jsonMessageFormatInstance

  it should "decode a string to json" in {
    jsonMessageFormatInstance.decode("""{"key1": "value1"}""").right.value should ===(
      Json.obj("key1" -> Json.fromString("value1"))
    )
  }

  it should "fail when decode a string is not valid json" in {
    jsonMessageFormatInstance.decode("""{"key2": value2}""").left.value.msg should include("expected json value")
  }

  it should "encode json to string" in {
    jsonMessageFormatInstance.encode(Json.obj("key4" -> Json.fromString("value4"))) should ===("""{"key4":"value4"}""")
  }
}

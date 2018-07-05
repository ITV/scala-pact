package com.itv.scalapact.argonaut62

import argonaut.Argonaut.jString
import argonaut.Json
import org.scalactic.TypeCheckedTripleEquals
import org.scalatest.{EitherValues, FlatSpec}
import org.scalatest.Matchers._

class IMessageFormatSpec extends FlatSpec with EitherValues with TypeCheckedTripleEquals {
  import com.itv.scalapact.json.jsonMessageFormatInstance

  it should "decode a string to json" in {
    jsonMessageFormatInstance.decode("""{"key1": "value1"}""").right.value should ===(
      Json.obj("key1" -> jString("value1"))
    )
  }

  it should "fail when decode a string is not valid json" in {
    jsonMessageFormatInstance.decode("""{"key2": value2}""").left.value.msg should include("Unexpected content found")
  }

  it should "encode json to string" in {
    jsonMessageFormatInstance.encode(Json.obj("key4" -> jString("value4"))) should ===("""{"key4":"value4"}""")
  }
}

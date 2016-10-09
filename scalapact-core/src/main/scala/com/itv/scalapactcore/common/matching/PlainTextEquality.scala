package com.itv.scalapactcore.common.matching

object PlainTextEquality {

  def check(expected: String, received: String): Boolean = {
    expected.trim == received.trim
  }

}

package com.itv.scalapact.shared

import java.util.Base64

case class BasicAuthenticationCredentials(username: String, password: String) {
  def asHeader: (String, String) = {
    val toEncode = s"$username:$password".getBytes
    "Authorization" -> s"Basic ${Base64.getEncoder.encodeToString(toEncode)}"
  }
}

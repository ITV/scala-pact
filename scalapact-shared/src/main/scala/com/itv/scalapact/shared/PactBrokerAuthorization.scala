package com.itv.scalapact.shared

import java.util.Base64

sealed trait PactBrokerAuthorization {
  def value: String
  def asHeader: (String, String) = "Authorization" -> value
}

object PactBrokerAuthorization {
  def apply(pactBrokerCredentials: (String, String), pactBrokerToken: String): Option[PactBrokerAuthorization] =
    (pactBrokerCredentials, pactBrokerToken) match {
      case ((username, password), _) if username.nonEmpty && password.nonEmpty =>
        Some(BasicAuthenticationCredentials(username, password))
      case ((_, _), token) if token.nonEmpty =>
        Some(BearerToken(token))
      case _ => None
    }

  private case class BasicAuthenticationCredentials(username: String, password: String) extends PactBrokerAuthorization {
    val value: String = {
      val toEncode = s"$username:$password".getBytes
      s"Basic ${Base64.getEncoder.encodeToString(toEncode)}"
    }
  }

  private case class BearerToken(token: String) extends PactBrokerAuthorization {
    val value: String = s"Bearer $token"
  }
}

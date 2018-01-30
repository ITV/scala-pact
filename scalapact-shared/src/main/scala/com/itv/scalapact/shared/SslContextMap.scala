package com.itv.scalapact.shared

import java.io.FileInputStream
import java.security.KeyStore
import javax.net.ssl.{KeyManagerFactory, SSLContext, TrustManagerFactory}
import com.itv.scalapact.shared.PactLogger

class SslContextMap(map: Map[String, SSLContext]) extends (Option[String] => Option[SSLContext]) {
  override def apply(optName: Option[String]): Option[SSLContext] = {
    val result = optName.map(name => map.getOrElse(name, throw new SslContextNotFoundException(name, this)))

    PactLogger.debug(s"SslContextMap($optName) ==> $result")

    if (SslContextMap.debugNones && optName.isEmpty) {
      try {
        throw new RuntimeException
      } catch {
        case e: Exception => e.printStackTrace()
      }
    }

    result
  }

  def legalValues: List[String] =
    map.keys.toList.sorted

  override def toString() =
    s"SslContextMap($map)"
}

class SslContextNotFoundException(name: String, sslContextMap: SslContextMap) extends Exception(s"SslContext [$name] not found. Legal values are [${sslContextMap.legalValues}]")

object SslContextMap {
  val sslContextHeaderName: String = "pact-ssl-context"

  var debugNones: Boolean = false

  implicit val defaultEmptyContextMap: SslContextMap = new SslContextMap(Map())

  def apply[T](simpleRequest: SimpleRequest)(fn: Option[SSLContext] => SimpleRequest => T)(implicit sslContextMap: SslContextMap): T = {
    val sslContext = sslContextMap(simpleRequest.sslContextName)
    val newRequest = simpleRequest.copy(headers = simpleRequest.headers - sslContextHeaderName)
    fn(sslContext)(newRequest)
  }

  def makeSslContext(keyStore: String, passphrase: String, trustStore: String, trustPassphrase: String): SSLContext = {
    val ksKeys = KeyStore.getInstance("JKS")
    ksKeys.load(new FileInputStream(keyStore), passphrase.toCharArray)

    val ksTrust = KeyStore.getInstance("JKS")
    ksTrust.load(new FileInputStream(trustStore), trustPassphrase.toCharArray)

    // KeyManagers decide which key material to use
    val kmf = KeyManagerFactory.getInstance("SunX509")
    kmf.init(ksKeys, passphrase.toCharArray)

    // TrustManagers decide whether to allow connections
    val tmf = TrustManagerFactory.getInstance("SunX509")
    tmf.init(ksTrust)

    val sslContext = SSLContext.getInstance("TLS")
    sslContext.init(kmf.getKeyManagers, tmf.getTrustManagers, null)
    sslContext
  }

}


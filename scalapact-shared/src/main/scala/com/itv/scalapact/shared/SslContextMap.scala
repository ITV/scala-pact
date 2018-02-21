package com.itv.scalapact.shared

import java.io.FileInputStream
import java.security.KeyStore
import javax.net.ssl.{KeyManagerFactory, SSLContext, TrustManagerFactory}

import scala.collection.concurrent.TrieMap

case class SSLContextData(keyManagerFactoryPassword: String, keyStore: String, passphrase: String, trustStore: String, trustPassphrase: String)

object SSLContextData {
  private var keyStore = "javax.net.ssl.keyStore"
  private var keyStorePass = "javax.net.ssl.keyStorePassword"
  private var truststore = "javax.net.ssl.trustStore"
  private var trustStorePassword = "javax.net.ssl.trustStorePassword"

  private def get(key: String) = {
    val result = System.getProperty(key)
    if (result == null) throw new NullPointerException(s"System property [$key] is null")
    result
  };
  def fromSystemProperties(): SSLContextData = SSLContextData(get(keyStorePass), get(keyStore), get(keyStorePass), get(truststore), get(trustStorePassword))

}

trait SSLContextDataToSslContext extends (SSLContextData => SSLContext)

object SSLContextDataToSslContext {

  def getSslContext(data: SSLContextData): SSLContext = {
    import data._
    val ksKeys: KeyStore = KeyStore.getInstance("JKS")
    val ksTrust = KeyStore.getInstance("JKS")
    def load(keyStore: KeyStore, store: String, password: String) = try {
      val inputStream = new FileInputStream(store)
      keyStore.load(inputStream, passphrase.toCharArray)
    } catch {
      case e: Exception => throw new IllegalArgumentException(s"Cannot load store at location [$store]", e)
    }
    load(ksKeys, keyStore, passphrase)
    load(ksTrust, keyStore, trustPassphrase)
    // KeyManagers decide which key material to use
    val kmf = KeyManagerFactory.getInstance("SunX509")
    kmf.init(ksKeys, keyManagerFactoryPassword.toCharArray)

    // TrustManagers decide whether to allow connections
    val tmf = TrustManagerFactory.getInstance("SunX509")
    tmf.init(ksTrust)

    val sslContext = SSLContext.getInstance("TLS")
    sslContext.init(kmf.getKeyManagers, tmf.getTrustManagers, null)
    sslContext
  }

  implicit object Default extends SSLContextDataToSslContext {
    override def apply(data: SSLContextData): SSLContext = getSslContext(data)
  }

}

case class ContextNameAndClientAuth(name: String, clientAuth: Boolean)

class SslContextMap(dataMap: Map[String, SSLContextData])(implicit sSLContextDataToSslContext: SSLContextDataToSslContext) {

  private val sslContextCache = new TrieMap[String, SSLContext]

  def getContext: String => SSLContext = { name => sslContextCache.getOrElseUpdate(name, sSLContextDataToSslContext(getData(name))) }

  def getData: String => SSLContextData = { name => dataMap.getOrElse(name, throw new SslContextNotFoundException(name, this)) }

  def legalValues: List[String] = dataMap.keys.toList.sorted

  override def toString() = s"SslContextMap($dataMap)"
}

class SslContextNotFoundException(name: String, sslContextMap: SslContextMap) extends Exception(s"SslContext [$name] not found. Legal values are [${sslContextMap.legalValues}]")

object SslContextMap {
  val sslContextHeaderName: String = "pact-ssl-context"

  var debugNones: Boolean = false

  implicit val defaultEmptyContextMap: SslContextMap = new SslContextMap(Map())

  def fromSystemProperties(name: String): SslContextMap = new SslContextMap(Map(name -> SSLContextData.fromSystemProperties()))


  def apply(specs: (String, (String, String, String, String, String))*): SslContextMap =
    new SslContextMap(specs.toMap.mapValues[SSLContextData]((SSLContextData.apply _).tupled))


  def apply[T](simpleRequest: SimpleRequest)(fn: Option[SSLContext] => SimpleRequest => T)(implicit sslContextMap: SslContextMap): T = {
    val optSslContext = simpleRequest.sslContextName.map(sslContextMap.getContext)
    val newRequest = simpleRequest.copy(headers = simpleRequest.headers - sslContextHeaderName)
    fn(optSslContext)(newRequest)
  }

}


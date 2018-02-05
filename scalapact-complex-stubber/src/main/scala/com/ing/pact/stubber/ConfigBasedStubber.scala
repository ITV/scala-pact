package com.ing.pact.stubber

import java.io.{File, FileFilter, FilenameFilter}
import java.util.ResourceBundle
import java.util.concurrent.{ExecutorService, Executors}
import javax.net.ssl.SSLContext

import com.itv.scalapact.shared._
import com.itv.scalapact.shared.http.PactStubService._
import com.itv.scalapactcore.common.PactReaderWriter._
import com.itv.scalapactcore.stubber.InteractionManager
import com.typesafe.config.{Config, ConfigFactory}
import org.http4s.server.Server
import org.http4s.server.blaze.BlazeBuilder

import scala.concurrent.ExecutionContext
import scala.concurrent.ExecutionContext.Implicits._
import scala.concurrent.duration._
import scala.io.Source

case class ServerSpec(name: String, port: Int, strict: Boolean, sslContextData: Option[SSLContextData], pacts: List[File], errorsAbort: Boolean) {
  def singleContextMap = sslContextData.fold(SslContextMap.defaultEmptyContextMap)(data => new SslContextMap(Map(name -> data)))
}


object ServerSpec {

  implicit object FromConfigForServerSpec extends FromConfigWithKey[ServerSpec] with Pimpers {

    override def apply(name: String, config: Config): ServerSpec = {
      ServerSpec(name,
        port = config.getInt("port"),
        strict = false,
        sslContextData = config.getOption[SSLContextData]("ssl-context"),
        pacts = config.getFiles("directory")(new FileFilter() {
          override def accept(pathname: File): Boolean = pathname.getName.endsWith(".json")
        }),
        errorsAbort = config.getBoolean("errorsAbort"))
    }

  }

  implicit class ServerSpecPimper(spec: ServerSpec)(implicit executorService: ExecutorService) extends Pimpers {
    def toBlaizeServer(pacts: Seq[Pact]): Option[Server] = {
      println(s"toBlaizeServer $spec and pacts are ${pacts.size}")
      pacts.nonEmpty.toOption {
        implicit val sslContextMap = spec.singleContextMap
        val interactionManager = pacts.foldLeft(new InteractionManager) { (im, t) => t.interactions ==> im.addInteractions; im }
        val result = BlazeBuilder
          .bindHttp(spec.port, "localhost").withServiceExecutor(executorService)
          //        .withExecutionContext(executionContext)
          .withIdleTimeout(60.seconds)
          .withOptionalSsl(spec.sslContextData.map(_ => spec.name))
          .withConnectorPoolSize(10)
          .mountService(ServiceMaker.service(interactionManager, spec.strict), "/")
          .run
        println(s"Started server on ${spec.port}")
        result
      }
    }
  }

  implicit object MessageFormatDataForServerSpec extends MessageFormatData[ServerSpec] with Pimpers {
    override def apply(spec: ServerSpec): Seq[String] = Seq(spec.name, spec.port.toString, spec.sslContextData.toString)
  }

  def loadPacts: ServerSpec => List[Either[String, Pact]] = { serverSpec: ServerSpec => serverSpec.pacts.map(Source.fromFile(_, "UTF-8").mkString).map(pactReader.jsonStringToPact) }

}

case class ServerSpecAndPacts(spec: ServerSpec, issuesAndPacts: List[Either[String, Pact]])


object ServerSpecAndPacts extends Pimpers {
  def printIssuesAndReturnPacts(title: String)(serverSpecAndPacts: ServerSpecAndPacts)(implicit resourceBundle: ResourceBundle): Seq[Pact] = {
    serverSpecAndPacts.issuesAndPacts.printWithTitle(title, ())
    serverSpecAndPacts.issuesAndPacts.values
  }
}


class ConfigBasedStubber(configFile: File)(implicit val resources: ResourceBundle, executorService: ExecutorService) extends Pimpers {
  "header.running".printlnFromBundle(configFile.getAbsoluteFile)


  implicit def errorStrategy[L, R](implicit spec: ServerSpec): ErrorStrategy[L, R] = spec.errorsAbort match {
    case false => ErrorStrategy.printErrorsAndUseGood("error.loading.server", spec.name)
    case true => ErrorStrategy.printErrorsAndAbort("error.loading.server", spec.name)
  }

  implicit object MessageFormatDataForRequest extends MessageFormatData[InteractionRequest] {
    override def apply(ir: InteractionRequest): Seq[String] = MessageFormatData((ir.method, ir.path))
  }

  def handleErrors(seq: Seq[Either[String, Pact]])(implicit spec: ServerSpec) = seq.handleErrors
  def printServerStarting(pacts: Seq[Pact])(implicit spec: ServerSpec) = pacts.ifNotEmpty("message.loading.server".printlnFromBundle((spec, pacts))) //.foreach { pact => pact.interactions.foreach { i => "message.pact.summary".printlnFromBundle(i.request) } }

  if (!configFile.exists())
    throw new IllegalArgumentException(s"File: ${configFile.getAbsolutePath} doesn't exist")

  configFile ==> ConfigFactory.parseFile ==> makeListFromConfig[ServerSpec](key = "servers") mapWith { implicit spec => ServerSpec.loadPacts ===> handleErrors =^> printServerStarting ===> spec.toBlaizeServer }

  def waitForever() = {
    while (true)
      Thread.sleep(10000000)
  }
}

object Stubber extends App with Pimpers {
  implicit val resources: ResourceBundle = ResourceBundle.getBundle("messages")
  val fileName: String = args match {
    case Array() => "stubber.cfg"
    case Array(fileName) => fileName
    case a => "error.usage".printlnFromBundle(()); System.exit(2); throw new RuntimeException()
  }
  implicit val executorService = Executors.newFixedThreadPool(10)
  new ConfigBasedStubber(new File(fileName)).waitForever()


}

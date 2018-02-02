package com.ing.pact.stubber

import java.io.File
import java.util.ResourceBundle
import javax.net.ssl.SSLContext

import com.itv.scalapact.shared.ColourOuput._
import com.itv.scalapact.shared._
import com.itv.scalapact.shared.http.PactStubService._
import com.itv.scalapactcore.common.PactReaderWriter._
import com.itv.scalapactcore.stubber.InteractionManager
import com.typesafe.config.{Config, ConfigFactory}
import org.http4s.server.blaze.BlazeBuilder

import scala.concurrent.ExecutionContext
import scala.concurrent.ExecutionContext.Implicits._
import scala.concurrent.duration._
import scala.io.Source

case class ServerSpec(name: String, port: Int, strict: Boolean, sslContext: Option[SSLContext], pacts: List[File], errorsAbort: Boolean)


object ServerSpec {

  implicit object FromConfigForServerSpec extends FromConfigWithKey[ServerSpec] with Pimpers {

    override def apply(name: String, config: Config): ServerSpec = {
      ServerSpec(name,
        port = config.getInt("port"),
        strict = false,
        sslContext = config.getOption[SSLContext]("ssl-context"),
        pacts = config.getFiles("directory")(_.getName.endsWith(".json")),
        errorsAbort = config.getBoolean("errorsAbort"))
    }

  }

  implicit class ServerSpecPimper(spec: ServerSpec)(implicit executionContext: ExecutionContext) extends Pimpers {
    def toBlaizeServer(pacts: Seq[Pact]) = pacts.isEmpty.toOption {
      val interactionManager = pacts.foldLeft(new InteractionManager) { (im, t) => t.interactions ==> im.addInteractions; im }
      BlazeBuilder
        .bindHttp(spec.port, "localhost")
        .withExecutionContext(executionContext)
        .withIdleTimeout(60.seconds)
        .withOptionalSsl(spec.sslContext)
        .withConnectorPoolSize(10)
        .mountService(ServiceMaker.service(interactionManager, spec.strict), "/")
        .run
    }
  }

  implicit object MessageFormatDataForServerSpec extends MessageFormatData[ServerSpec] with Pimpers {
    override def apply(spec: ServerSpec): Seq[String] = Seq(spec.name, spec.port.toString, spec.sslContext.asString("http", _ => "https"))
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

object Stubber extends App with Pimpers {
  implicit val resources: ResourceBundle = ResourceBundle.getBundle("messages")
  PactLogger.message(resources.getString("header.running"))

  implicit def errorStrategy[L, R](implicit spec: ServerSpec): ErrorStrategy[L, R] = spec.errorsAbort match {
    case false => ErrorStrategy.printErrorsAndUseGood("error.loading.server", spec.name)
    case true => ErrorStrategy.printErrorsAndAbort("error.loading.server", spec.name)
  }

  implicit object MessageFormatDataForRequest extends MessageFormatData[InteractionRequest] {
    override def apply(ir: InteractionRequest): Seq[String] = MessageFormatData((ir.method, ir.path))
  }

  def handleErrors(seq: Seq[Either[String, Pact]])(implicit spec: ServerSpec) = seq.handleErrors
  def printServerStarting(pacts: Seq[Pact])(implicit spec: ServerSpec) = pacts.ifNotEmpty("message.loading.server".printlnFromBundle((spec, pacts)))//.foreach { pact => pact.interactions.foreach { i => "message.pact.summary".printlnFromBundle(i.request) } }

  new File("scalapact-complex-stubber/stubber.cfg") ==> ConfigFactory.parseFile ==> makeListFromConfig[ServerSpec](key = "servers") mapWith { implicit spec => ServerSpec.loadPacts ===> handleErrors =^> printServerStarting ===> spec.toBlaizeServer }

  while (true)
    Thread.sleep(10000000)

}

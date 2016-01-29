package com.itv.scalapact

import java.io.{PrintWriter, File}

import org.json4s.DefaultFormats
import org.json4s.native.Serialization._
import org.json4s.native.JsonParser._

import scala.language.implicitConversions

object ScalaPactContractWriter {

  private implicit val formats = DefaultFormats

  private val simplifyName: String => String = name =>
    "[^a-zA-Z0-9-]".r.replaceAllIn(name.replace(" ", "-"), "")

  val writePactContracts: DescribesPactBetween => Unit = pactDescription => {
    val dirPath = "target/pacts"
    val dirFile = new File(dirPath)

    if (!dirFile.exists()) {
      dirFile.mkdir()
    }

    val relativePath = dirPath + "/" + simplifyName(pactDescription.consumer) + "-" + simplifyName(pactDescription.provider) + ".json"
    val file = new File(relativePath)

    if (file.exists()) {
      file.delete()
    }

    file.createNewFile()

    new PrintWriter(relativePath) {
      write(producePactJson(pactDescription))
      close()
    }

    ()
  }

  private def producePactJson(pactDescription: DescribesPactBetween): String =
    writePretty(
      Pact(
        provider = PactActor(pactDescription.provider),
        consumer = PactActor(pactDescription.consumer),
        interactions = pactDescription.interactions.map { i =>

          val formatBody: Map[String, String] => String => Option[AnyRef] = headers => body =>
            if(headers.exists(p => p._1.toLowerCase == "content-type" && p._2.contains("json"))) jsonStringToAnyRef(body)
            else body

          val requestBody = formatBody(i.request.headers)(i.request.body)
          val responseBody = formatBody(i.response.headers)(i.response.body)

          Interaction(
            providerState = i.given,
            description = i.description,
            request = InteractionRequest(
              method = i.request.method.method,
              path = i.request.path,
              headers = i.request.headers,
              body = requestBody
            ),
            response = InteractionResponse(
              status = i.response.status,
              headers = i.response.headers,
              body = responseBody
            )
          )
        }
      )
    )

  private def jsonStringToAnyRef(maybeJsonString: Option[String]): Option[AnyRef] = maybeJsonString.map(parse)

  implicit private val intToBoolean: Int => Boolean = v => v > 0
  implicit private val stringToBoolean: String => Boolean = v => v != ""
  implicit private val mapToBoolean: Map[String, String] => Boolean = v => v.nonEmpty

  implicit private def valueToOptional[A](value: A)(implicit p: A => Boolean): Option[A] = if(p(value)) Option(value) else None

}

case class Pact(provider: PactActor, consumer: PactActor, interactions: List[Interaction])
case class PactActor(name: String)
case class Interaction(providerState: Option[String], description: String, request: InteractionRequest, response: InteractionResponse)
case class InteractionRequest(method: Option[String], path: Option[String], headers: Option[Map[String, String]], body: Option[AnyRef])
case class InteractionResponse(status: Option[Int], headers: Option[Map[String, String]], body: Option[AnyRef])

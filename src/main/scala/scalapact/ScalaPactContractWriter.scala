package scalapact

import java.io.{PrintWriter, File}

import org.json4s.DefaultFormats
import org.json4s.native.Serialization._
import org.json4s.native.JsonParser._

import scala.language.implicitConversions

object ScalaPactContractWriter {

  private implicit val formats = DefaultFormats

  val writePactContracts: DescribesPactBetween => Unit = pactDescription => {
    val dirPath = "target/pacts"
    val dirFile = new File(dirPath)

    if (!dirFile.exists()) {
      dirFile.mkdir()
    }

    val relativePath = dirPath + "/" + pactDescription.consumer + "-" + pactDescription.provider + ".json"
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

          val body =
            if(i.response.headers.exists(p => p._1.toLowerCase == "content-type" && p._2.contains("json"))) //TODO: A more robust check?
              jsonStringToAnyRef(i.response.body)
            else
              Option(i.response.body)

          Interaction(
            providerState = i.given,
            description = i.description,
            request = InteractionRequest(
              method = i.request.method.method,
              path = i.request.path,
              headers = i.request.headers
            ),
            response = InteractionResponse(
              status = i.response.status,
              headers = i.response.headers,
              body = body
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
case class InteractionRequest(method: Option[String], path: Option[String], headers: Option[Map[String, String]])
case class InteractionResponse(status: Option[Int], headers: Option[Map[String, String]], body: Option[AnyRef])

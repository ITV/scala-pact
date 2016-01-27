package scalapact.pactspec.util

import org.json4s.DefaultFormats
import org.json4s.native.JsonParser._

import scala.io.Source

object PactSpecLoader {

  private implicit val formats = DefaultFormats

  def fromResource(path: String): String =
    Source.fromURL(getClass.getResource("/pact-specification-version-2/testcases/" + path)).getLines().mkString("\n")

  def parseInto[T](json: String)(implicit mf: Manifest[T]): T = {
    parse(json).extract[T]
  }

}

package com.itv.scalapact.shared.matchir

import com.itv.scalapact.shared.matchir.IrNodePath.IrNodePathEmpty
import com.itv.scalapact.shared.matchir.PactPathParseResult.{PactPathParseFailure, PactPathParseSuccess}

import scala.annotation.tailrec
import scala.util.matching.Regex

/** PactPath (defined in the pact standard) is JsonPath with a few tweaks to support
  * querying XML with a nearly JsonPath-like syntax. Specific modifications to JsonPath are:
  *
  * - names match to element names ($.body.animals maps to <animals>)
  * - @names match to attribute names
  * - #text match to the text elements
  *
  * JsonPath support a ["xxx"] form which we use for to escape the @ and #. e.g.
  * foo.bar["#text"]
  * foo.bar['@id']
  */
object PactPath {
  import PactPathPatterns._

  val toPactPath: IrNodePath => String = _.renderAsString

  val fromPactPath: String => PactPathParseResult = pactPath => {
    @tailrec def rec(remaining: String, acc: IrNodePath): PactPathParseResult =
      remaining match {
        // Complete, success
        case "" =>
          PactPathParseSuccess(acc)

        // Prefixes
        case dollarPrefix(_, r) =>
          rec(r, acc)

        // Fields
        case fieldNamePrefix(_, r) =>
          rec(r, acc)

        case fieldName(name, r) =>
          rec(r, acc <~ name)

        case fieldNameSuffix(_, r) =>
          rec(r, acc)

        // Arrays
        case arrayAnyElement(_, r) =>
          rec(r, acc <~ "*")

        case arrayElementAtIndex(i, r) =>
          safeStringToInt(i) match {
            case Some(index) =>
              rec(r, acc <~ index)

            case None =>
              PactPathParseFailure(pactPath, remaining, Some(s"Could not convert '$i' to array index."))
          }

        // XML Addons
        case xmlAttributeName(attributeName, r) =>
          rec(r, acc <@ attributeName)

        case xmlTextElement(_, r) =>
          rec(r, acc.text)

        // Suffixes
        case anyField(_, r) =>
          rec(r, acc.<*)

        // Unmatched, failure
        case _ =>
          PactPathParseFailure(pactPath, remaining, None)

      }

    rec(pactPath, IrNodePathEmpty)
  }

  private lazy val safeStringToInt: String => Option[Int] = str =>
    try Option(str.toInt)
    catch {
      case _: Throwable => None
    }

  object PactPathPatterns {
    // Prefixes
    val dollarPrefix: Regex = """^(\$.body|\$.headers)(.*)$""".r

    // Fields
    val fieldNamePrefix: Regex = """^(\.|\[[\'|\"])(.*)$""".r
    val fieldName: Regex =
      """^([a-zA-Z0-9:\-_]+)(.*)$""".r // TODO: This is very limited, technically any escaped string is valid
    val fieldNameSuffix: Regex = """^([\'|\"]\])(.*)$""".r

    // Arrays
    val arrayAnyElement: Regex     = """^(\[\*\])(.*)$""".r
    val arrayElementAtIndex: Regex = """^\[(\d+)\](.*)$""".r

    // Xml addons
    val xmlAttributeName: Regex = """^@([a-zA-Z0-9:\-_]+)(.*)$""".r
    val xmlTextElement: Regex   = """^#([a-zA-Z0-9:\-_]+)(.*)$""".r

    // Suffixes
    val anyField: Regex = """^(\*)(.*)$""".r
  }
}

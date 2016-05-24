package com.itv.scalapactcore.common

import scala.language.implicitConversions
import scala.xml.{Elem, Node}

import scalaz._
import Scalaz._

object PermissiveXmlEquality {

  implicit def toJsonEqualityWrapper(json: Elem): XmlEqualityWrapper = XmlEqualityWrapper(json)

  case class XmlEqualityWrapper(json: Elem) {
    def =~(to: Elem): Boolean = PermissiveXmlEqualityHelper.areEqual(json, to)
  }
}


object PermissiveXmlEqualityHelper {

  /***
    * Permissive equality means that the elements and fields defined in the 'expected'
    * are required to be present in the 'received', however, extra elements on the right
    * are allowed and ignored. Additionally elements are still considered equal if their
    * fields or array elements are out of order, as long as they are present since json
    * doesn't not guarantee element order.
    */
  def areEqual(expected: Elem, received: Elem): Boolean = {

    println("------")
    println(expected)

    //println(expected.prefix) //namespace
    println(expected.isEmpty)
    println(expected.isAtom)
    //println(expected.label)
    println(expected.child)
    //println(expected.child.length)
    println(expected.attributes.asAttrMap)

    println(<foo>foo text</foo>.child)
    println(<foo>foo text</foo>.child.isEmpty)
    println(<foo/>.isAtom)

//    val prefixEqual = expected.prefix == received.prefix
//    val labelEqual = expected.label == received.label
//    val attributesEqual = mapContainsMap(expected.attributes.asAttrMap)(received.attributes.asAttrMap)
//    val childLengthOk = expected.child.length <= received.child.length
//
//    val nodeEqual = prefixEqual && labelEqual && attributesEqual && childLengthOk

    // For each node:
    // node name is the same
    // contains all attributes
    // has at least the same number of child nodes
    // if no child nodes, should be equal



//    expected match {
//      case j: Json if j.isObject && received.isObject =>
//        compareFields(expected, received, j.objectFieldsOrEmpty)
//
//      case j: Json if j.isArray && received.isArray =>
//
//        (j.array |@| received.array) { (ja, ra) =>
//          ja.forall { jo =>
//            ra.exists(ro => areEqual(jo, ro))
//          }
//        } match {
//          case Some(matches) => matches
//          case None => false
//        }
//
//      case j: Json =>
//        expected == received
//    }

    (expected.headOption |@| received.headOption) { (e, r) => compareNodes(e)(r) } match {
      case Some(bool) => bool
      case None => false
    }
  }

  lazy val mapContainsMap: Map[String, String] => Map[String, String] => Boolean = e => r =>
    e.forall { ee =>
      r.exists(rr => rr._1 == ee._1 && rr._2 == ee._2)
    }

  lazy val compareNodes: Node => Node => Boolean = expected => received => {
    val prefixEqual = expected.prefix == received.prefix
    val labelEqual = expected.label == received.label
    val attributesEqual = mapContainsMap(expected.attributes.asAttrMap)(received.attributes.asAttrMap)
    val childLengthOk = expected.child.length <= received.child.length

    val childrenEqual =
      if(expected.isEmpty) {
        expected.text == received.text
      } else {
        println("We have kids!!")
        false
      }

    prefixEqual && labelEqual && attributesEqual && childLengthOk && childrenEqual
  }

//  private def compareFields(expected: Json, received: Json, expectedFields: List[Json.JsonField]): Boolean = {
//    if(!expectedFields.forall(f => received.hasField(f))) false
//    else {
//      expectedFields.forall { field =>
//
//        (expected.field(field) |@| received.field(field)){ areEqual(_, _) } match {
//          case Some(bool) => bool
//          case None => false
//        }
//      }
//    }
//  }

}

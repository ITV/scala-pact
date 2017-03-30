package com.itv.scalapactcore.common.matchir

import org.scalatest.{FunSpec, Matchers}

import scala.language.implicitConversions

class MatchIRSpec extends FunSpec with Matchers {

  implicit def toOption[A](v: A): Option[A] = Option(v)

  describe("Converting XML to MatchIR") {

    it("should be able to convert one node") {

      val xml: String = <fish></fish>.toString()

      val ir: Option[IrNode] = IrNode("fish", None, Map(), None, Nil)

      MatchIR.fromXml(xml) shouldEqual ir

    }

    it("should be able to convert one node with content") {

      val xml: String = <fish>haddock</fish>.toString()

      val ir: Option[IrNode] = IrNode("fish", None, Map(), Some(IrStringNode("haddock")), Nil)

      MatchIR.fromXml(xml) shouldEqual ir

    }

    it("should be able to convert one node with a namespace") {

      val xml: String = <ns1:fish>haddock</ns1:fish>.toString()

      val ir: Option[IrNode] = IrNode("fish", "ns1", Map(), Some(IrStringNode("haddock")), Nil)

      MatchIR.fromXml(xml) shouldEqual ir

    }

    it("should be able to convert one node with attributes") {

      val xml: String = <fish id="3" description="A fish" endangered="false"></fish>.toString()

      val ir: Option[IrNode] = IrNode(
        "fish",
        None,
        Map("id" -> IrNumberNode(3), "description" -> IrStringNode("A fish"), "endangered" -> IrBooleanNode(false)),
        None,
        Nil
      )

      MatchIR.fromXml(xml) shouldEqual ir

    }

    it("should be able to convert two nested nodes and a value") {

      val xml: String = <fish><breed>cod</breed></fish>.toString()

      val ir: Option[IrNode] = IrNode("fish", None, Map(), None,
        List(
          IrNode("breed", None, Map(), Some(IrStringNode("cod")), Nil)
        )
      )

      MatchIR.fromXml(xml) shouldEqual ir

    }

  }

  describe("Converting JSON to MatchIR") {

    it("should be able to convert one node") {

      println("----JSON-----")
      println(MatchIR.fromJSON("""{"configStatus":{"localOverride":{"exists":true,"path":"/etc/itv/thor.json"},"environmentSpecific":{"exists":true,"path":"/etc/itv/thor-env.json"}},"appVersion":"0.1.1490204916862.27","odinStatus":{"statusCode":-1,"message":"Odin status not checked"},"healthy":true,"jordStatus":{"statusCode":-1,"message":"Jord status not checked"}}""").map(_.renderAsString()).getOrElse(""))
      println("----XML 1 ------")
      val x = MatchIR.fromXml("""<soapenv:Envelope xmlns:ns6="http://ref.itvplc.ads/Schema/Syndication/Service/PackageImageFacade/1.0" xmlns:ns5="http://ref.itvplc.ads/Schema/Syndication/Service/PackageAssetFacade/1.0" xmlns:pac="http://ref.itvplc.ads/PackageAssetsPrePublish" xmlns:ns4="http://ref.itvplc.ads/Schema/Syndication/Partners/Common/Common/BusinessObjects/1.0" xmlns:ns3="http://ref.itvplc.ads/Schema/Syndication/Event/PackageEvtMsgDetails/1.0" xmlns:ns2="http://ref.itvplc.ads/Schema/Common/Header/1.0" xmlns:ns1="http://ref.itvplc.ads/Schema/Syndication/Event/Package/1.0" xmlns:ns="http://ref.itvplc.ads/Schema/Syndication/PackageEventPublish/1.0" xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/">
                                |      <soapenv:Header/>
                                |      <soapenv:Body>
                                |        <ns:PackageEventPublishProcessRequest>
                                |          <ns1:PackageEvent>
                                |            <ns2:HeaderRq>
                                |              <ns2:RqUID/>
                                |              <ns2:Action/>
                                |            </ns2:HeaderRq>
                                |            <ns3:PackageHeader>
                                |              <ns3:PackageInstanceId/>
                                |              <ns3:PackageName/>
                                |              <ns3:Operation/>
                                |            </ns3:PackageHeader>
                                |            <ns3:PackageEventMessage>
                                |              <ns3:JVMetadataEvtMsg>
                                |                <ns4:MetadataInqRq>
                                |                  <ns2:HeaderRq>
                                |                    <ns2:RqUID>123456</ns2:RqUID>
                                |                    <ns2:Action>Add</ns2:Action>
                                |                  </ns2:HeaderRq>
                                |                  <ns4:ProductionId>2/4663/0003#001</ns4:ProductionId>
                                |                  <ns4:PlatformType>CTV</ns4:PlatformType>
                                |                  <ns4:PartnerId>Freesat</ns4:PartnerId>
                                |                  <ns4:MetadataType>Update</ns4:MetadataType>
                                |                </ns4:MetadataInqRq>
                                |              </ns3:JVMetadataEvtMsg>
                                |            </ns3:PackageEventMessage>
                                |            <ns3:EventName>JVMetadata</ns3:EventName>
                                |            <ns3:PackageNameValue>
                                |              <ns3:Name/>
                                |              <ns3:Value/>
                                |            </ns3:PackageNameValue>
                                |          </ns1:PackageEvent>
                                |        </ns:PackageEventPublishProcessRequest>
                                |      </soapenv:Body>
                                |    </soapenv:Envelope>""".stripMargin)

      println(x.getOrElse(""))
      println("----XML 2------")
      println(x.map(_.renderAsString()).getOrElse(""))
      println("---------")

      val json: String =
        """
          |{
          |  "fish": {}
          |}
        """.stripMargin

      val ir: Option[IrNode] = IrNode("", None, Map(), None,
        List(
          IrNode("fish", None, Map(), None, Nil)
        )
      )

      MatchIR.fromJSON(json) shouldEqual ir

    }

    it("should be able to convert two nested nodes and a value") {

      val json: String =
        """
          |{
          |  "fish": {
          |    "breed": "cod"
          |  }
          |}
        """.stripMargin

      val ir: Option[IrNode] = IrNode("", None, Map(), None,
        List(
          IrNode("fish", None, Map(), None,
            List(
              IrNode("breed", None, Map(), Some(IrStringNode("cod")), Nil)
            )
          )
        )
      )

      MatchIR.fromJSON(json) shouldEqual ir

    }

    it("should convert a simple top level array") {

      val json: String =
        """
          |[1,2,3]
        """.stripMargin

      val ir: Option[IrNode] = Option {
        IrNode("", None, Map(), None,
          List(
            IrNode("", None, Map(), Some(IrNumberNode(1)), Nil),
            IrNode("", None, Map(), Some(IrNumberNode(2)), Nil),
            IrNode("", None, Map(), Some(IrNumberNode(3)), Nil)
          )
        )
      }

      MatchIR.fromJSON(json) shouldEqual ir

    }

    it("should be able to convert a top level array with two nodes") {

      val json: String =
        """
          |[
          |  {
          |    "fish": {
          |      "breed": "cod"
          |    }
          |  },
          |  {
          |    "fish": {
          |      "breed": "haddock"
          |    }
          |  }
          |]
        """.stripMargin

      val ir: Option[IrNode] =
        Option {
          IrNode("", // This is the top level empty array
            None,
            Map(),
            None,
            List(
              IrNode("", // the top level array's name (empty) is propagated to the children
                None,
                Map(),
                None,
                List(
                  IrNode(
                    "fish",
                    None,
                    Map(),
                    None,
                    List(
                      IrNode("breed", None, Map(), Some(IrStringNode("cod")), Nil)
                    )
                  )
                )
              ),
              IrNode("",
                None,
                Map(),
                None,
                List(
                  IrNode(
                    "fish",
                    None,
                    Map(),
                    None,
                    List(
                      IrNode("breed", None, Map(), Some(IrStringNode("haddock")), Nil)
                    )
                  )
                )
              )
            )
          )

        }

      MatchIR.fromJSON(json) shouldEqual ir

    }

  }

}

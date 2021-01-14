import scala.concurrent.duration._

import scala.io.Source

val tag = {
  val source = Source.fromFile("tag.txt")
  val t = source.getLines().mkString
  source.close()
  t
}

pactBrokerAddress := "https://test.pact.dius.com.au"
//For publishing to pact-broker test server (these credentials are public knowledge)
pactBrokerCredentials := ("dXfltyFMgNOFZAxr8io9wJ37iUpY42M", "O5AIZWxelWbLvqMd8PkAVycBJh2Psyg1")
pactContractVersion := "0.0.1"
pactContractTags := Seq(tag)
pactBrokerClientTimeout := 5.seconds
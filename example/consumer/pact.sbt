//For publishing to pact-broker test server (these credentials are public knowledge)
import scala.concurrent.duration._

import scala.concurrent.duration._

pactBrokerAddress := "https://test.pact.dius.com.au"
pactBrokerCredentials := ("dXfltyFMgNOFZAxr8io9wJ37iUpY42M", "O5AIZWxelWbLvqMd8PkAVycBJh2Psyg1")
pactContractVersion := "0.2.0"
pactContractTags := Seq("example")
pactBrokerClientTimeout := 5.seconds
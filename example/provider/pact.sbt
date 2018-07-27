
import java.io.PrintWriter

import com.itv.scalapact.plugin._
import com.itv.scalapact.shared.InteractionRequest
import com.itv.scalapactcore.verifier.Verifier.ProviderStateResult

import scala.concurrent.duration._

scalaPactEnv :=
  ScalaPactEnv.defaults
    .withPort(8080)
    .withHost("localhost")
    .withLocalPactFilePath("delivered_pacts/")
    .withClientTimeOut(2.seconds)

// New style
providerStateMatcher := {
  case "Results: Bob, Fred, Harry" =>
    val peopleFile = new File("people.txt")
    peopleFile.createNewFile()
    peopleFile.setWritable(true)

    val writer = new PrintWriter(peopleFile)
    writer.print("Bob,Fred,Harry")
    writer.close()

    val newHeader = "Pact" -> "modifiedRequest"
    ProviderStateResult(true, { req: InteractionRequest => req.copy(headers = Option(req.headers.fold(Map(newHeader))(_ + newHeader))) })
}

// Old style - still supported
// providerStates := Seq(
//   ("Results: Bob, Fred, Harry", (key: String) => {
//
//     val peopleFile = new File("people.txt")
//     peopleFile.createNewFile()
//     peopleFile.setWritable(true)
//
//     val writer = new PrintWriter(peopleFile)
//     writer.print("Bob,Fred,Harry")
//     writer.close()
//
//     true
//   })
// )
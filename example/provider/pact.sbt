import java.io.PrintWriter

import com.itv.scalapact.plugin.ScalaPactPlugin._

// New style
providerStateMatcher := {
  case "Results: Bob, Fred, Harry" =>

    val peopleFile = new File("people.txt")
    peopleFile.createNewFile()
    peopleFile.setWritable(true)

    val writer = new PrintWriter(peopleFile)
    writer.print("Bob,Fred,Harry")
    writer.close()

    true
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

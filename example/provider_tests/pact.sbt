import java.io.PrintWriter

import com.itv.scalapact.plugin.ScalaPactPlugin._

providerStates := Seq(
  ("Results: Bob, Fred, Harry", (key: String) => {

    val peopleFile = new File("people.txt")
    peopleFile.createNewFile()
    peopleFile.setWritable(true)

    val writer = new PrintWriter(peopleFile)
    writer.print("Bob,Fred,Harry")
    writer.close()

    true
  })
)

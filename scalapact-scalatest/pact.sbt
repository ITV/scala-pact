import com.itv.scalapact.plugin.ScalaPactPlugin._
import com.itv.scalapactcore.common._

val Resource = """Resource:([a-z]+)\{([^\{\}]+)\}""".r

providerStates := {
  case key: String if key.startsWith("Resource with ID 1234 exists") =>
    println("Injecting key 1234 into the database...")
    // Do some work to ensure the system under test is
    // in an appropriate state before verification

    true
  case Resource("user", id: String) => // Sample 'Resource:User:id:1234'
    true

}
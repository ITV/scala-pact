
addSbtPlugin("org.xerial.sbt" % "sbt-sonatype" % "1.1")

addSbtPlugin("com.jsuereth" % "sbt-pgp" % "1.1.0")

addSbtPlugin("org.wartremover" % "sbt-wartremover" % "2.2.1")

addSbtPlugin("com.lightbend.paradox" % "sbt-paradox" % "0.3.0")

resolvers += "jgit-repo" at "http://download.eclipse.org/jgit/maven"

addSbtPlugin("com.typesafe.sbt" % "sbt-ghpages" % "0.6.2")

addSbtPlugin("com.typesafe.sbt" % "sbt-site" % "1.3.1")

addSbtPlugin("com.lucidchart" % "sbt-cross" % "3.0")
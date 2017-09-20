
addSbtPlugin("org.xerial.sbt" % "sbt-sonatype" % "1.1")

//addSbtPlugin("org.wartremover" % "sbt-wartremover" % "1.2.1")

addSbtPlugin("com.lightbend.paradox" % "sbt-paradox" % "0.2.8")

resolvers += "jgit-repo" at "http://download.eclipse.org/jgit/maven"

addSbtPlugin("com.typesafe.sbt" % "sbt-ghpages" % "0.5.4")

addSbtPlugin("com.typesafe.sbt" % "sbt-site" % "1.2.0")

addSbtPlugin("com.eed3si9n" % "sbt-assembly" % "0.14.1")
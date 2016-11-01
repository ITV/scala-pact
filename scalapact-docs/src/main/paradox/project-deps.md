# Libraries and dependencies
The Pact integration test library itself depends on other of Scala libraries to make it work.

## Java 8
Scala-Pact is currently only compiled to Java 8.

## Http4s (0.14.11a)
The dependency is on [Http4s](http://http4s.org/) which is used by the stubber, verifier and test frameworks.

Http4s also pulls in some of it's own dependencies that Scala-Pact makes use of. These are:

#### Argonaut
The Http4s 0.14.11a flavour of [Argonaut](http://argonaut.io/) is also used by Scala-Pact to read and write the Pact files.

#### Scalaz
The Http4s 0.14.11a flavour of [Scalaz](https://github.com/scalaz/scalaz) which is the glue that holds Http4s together, but Scala-Pact has no direct dependency on it.

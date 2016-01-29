# ScalaPact
Library for generating Consumer Driven Contract files in Scala projects following the PACT standard using [ScalaTest](http://www.scalatest.org/).

## Motivation
[Pact](https://github.com/realestate-com-au/pact) is an implementation of CDC testing ([Consumer Driven Contract testing](http://martinfowler.com/articles/consumerDrivenContracts.html)). There are other implementations like [Pacto](https://github.com/thoughtworks/pacto) and they vary slightly in how they interpret the testing process.

Following the Pact implementation of CDC, the process goes something like this:
1. Write a piece of client code in a service consumer project that knows the details of how to connect to a provider.
2. Write a real integration test for that client code that hits a mock, but also as a side effect emits a JSON file describing the relationship. This JSON file is your Pact or Contract file. Note that in Pact (not Pacto) it is owned by the consumer.
3. During development of the consumer, the pact file can be used to run a lightweight stub service that mimics the expected behavior of the provider.
4. Give the generated pact file to the team who build the provider, an upstream service you depend on. This pact file tells the provider team both how you expect their API to behave and which parts of their API are delivering value to you the consumer.
5. The provider then verifies the pact by running the requests and responses described in the pact file against their system.

Most of the tools for CDC testing have come out of the Ruby community with a few other implementations for other languages here and there. The crucial point to appreciate is that the client code is written in your native language, in your native languages test framework and therefore your pact integrations tests should be too. The other uses of the pact files, verification and stubbing are largely standalone processes that are language agnostic and so it's quite feasible to use the Ruby implementations.

There is an implementation of the pact integration test suite that is theoretically compatible with Scala and [Specs2](https://etorreborre.github.io/specs2/) called [Pact-JVM](https://github.com/DiUS/pact-jvm). Pact-JVM also theoretically supports other JVM languages like Java and Groovy.

In practice we had found there were issues with Pact-JVM integration test solution for Scala (via Specs2). There was a choice between investing time to help fix Pact-JVM or writing a replacement library. As Pact-JVM is a general purpose library across the JVM, working on it was an unknown quantity and it was deemed worth prototyping a replacement. ScalaPact is the result.

## Setup
ScalaPact is not a published library yet. For the time being, we recommend that you checkout the project and do a local SBT publish in order to use it as a dependency. Alternatively you can compile the JAR and place it manually in the unmanaged lib folder of your project.

If you decide to do a local publish, you will then need to add the dependency to your build.sbt file like this:

```
libraryDependencies ++= Seq(
  "com.itv" %% "scalapact" % "0.0.1-SNAPSHOT" % "test"
)
```

## Basic usage examples
Coming soon. The API is still being worked on!

## Documentation TODO's
- How to use
- Examples
- Links to other pact resources
- Running a stub service
- Pact proxy provider to verify

## Development TODO's
- Publish lib to repo
- Improve error reporting
- Go back to immutability and add a key to pacts to generate multiple files, also then need to improve test script
- Normalise consumer & provider names before using them as the file name
- Improve builder so that case class public vars are not visible during build

## Known issues
- Each tests runs all the accumulated tests
- Only supported header is content-type

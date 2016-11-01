# Motivation
[Pact](https://github.com/realestate-com-au/pact) is an implementation of CDC testing ([Consumer Driven Contract testing](http://martinfowler.com/articles/consumerDrivenContracts.html)). There are other implementations like [Pacto](https://github.com/thoughtworks/pacto) and they vary slightly in how they interpret the testing process.

Following the Pact interpretation of CDC, the process goes something like this:

1. Write a piece of client code in a project that consumes a service and knows the details of how to connect to that provider.
1. Write a real integration test for that client code that hits a mock, but also as a side effect emits a JSON file describing the relationship. This JSON file is your Pact contract file. Note that in Pact (not Pacto) it is owned by the consumer.
1. During development of the consumer, the Pact file can be used to run a lightweight stub service that mimics the expected behavior of the provider. *Note: The provider need not even exist yet and the Pact files can form part of the providers design specification.*
1. Give the generated Pact file to the team that build the provider, an upstream service you depend on. This Pact file tells the provider team both how you expect their API to behave and *which parts of their API are delivering value to you the consumer*.
1. The provider then verifies the Pact by running the requests and responses described in the Pact file against their system.

Most of the original tools for CDC testing have come out of the Ruby community but a full list of officially supported languages can be found on the [pact.io](http://docs.pact.io/) website.

The crucial point to appreciate is that the client code is written in your native language using your normal test framework, and therefore, your Pact integrations tests must also be written in your native language too!

The other uses of the Pact files, verification and stubbing are largely standalone processes that are language agnostic and so it's quite feasible to use the Ruby implementations if you prefer.

There is another implementation of the Pact integration test suite that is compatible with Scala and [Specs2](https://etorreborre.github.io/specs2/) called [Pact-JVM](https://github.com/DiUS/pact-jvm). Pact-JVM also supports other JVM languages like Java and Groovy. Scala-Pact has the same aims and adheres to the same standards but attempts to deliver a more Scala specific experience.

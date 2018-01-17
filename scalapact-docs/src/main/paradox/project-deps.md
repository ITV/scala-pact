# Dependencies
The Pact integration test library itself depends on other of Scala libraries to make it work. Specifically it needs an Http and a JSON library.

## Scala-Pact 2.2.x
Versions of Scala-Pact have their dependencies baked in. The benefit was that it made development of the library initially easy, but as more people tried to adopt it, reports came in that it was causing massive dependency conflicts for fairly obvious reasons.

To tackle this, Scala-Pact now supports a range of libraries and versions. We can add more, and @ref:[pull-requests are welcome](contributing.md).

As far as the user is concerned there are two things to be aware of:
1. Which combination of libraries all work with Scala-Pact and your project;
2. That you need to explicitly add them to your project (see set up guides).

### Compatibility Matrix
Please note that this is for SBT 13.x. Once the upgrade to SBT 1.x (with Scala 2.12.3 compatible plugins) is complete this table will change. For instance, Circe is not very useful at the moment, but will be soon!
```
                    |   Plugin  |             Framework
-------------------------------------------------------------------
                    |  (2.10.6) |  (2.10.6) | (2.11.11) |  (2.12.3)
-------------------------------------------------------------------
Http4s    0.15.16a  |    YES    |    YES    |    YES    |    YES
Http4s    0.16.6    |    YES    |    YES    |    YES    |    YES
Http4s    0.16.6a   |    YES    |    YES    |    YES    |    YES
Http4s    0.17.6    |    NO     |    NO     |    YES    |    YES
Http4s    0.18.0-M8 |    NO     |    NO     |    YES    |    YES
Argonaut  6.1       |    YES    |    YES    |    YES    |    NO
Argonaut  6.2       |    YES    |    YES    |    YES    |    YES
Circe     0.8       |    NO     |    NO     |    YES    |    YES
Circe     0.9       |    NO     |    NO     |    YES    |    YES
```

At the moment it is best to avoid Http4s 0.17.0+, Circe 0.8+ and Argonaut 6.1 if possible.

## Prior to version 2.2.0
These dependencies were baked in as follows:

#### Http4s 0.15.0a
The dependency is on [Http4s](http://http4s.org/) which is used by the stubber, verifier and test frameworks.

Http4s also pulls in some of it's own dependencies that Scala-Pact makes use of. These are:

#### Argonaut (supplied by Http4s 0.15.0a)
The Http4s 0.15.0a flavour of [Argonaut](http://argonaut.io/) is also used by Scala-Pact to read and write the Pact files.

#### Scalaz (supplied by Http4s 0.15.0a)
The Http4s 0.15.0a flavour of [Scalaz](https://github.com/scalaz/scalaz) which is the glue that holds Http4s together, but Scala-Pact has no direct dependency on it.

# Basic usage

@@@ index

* [SBT Tasks](sbt-tasks.md)
* [SBT Commands](sbt-commands.md)
* [Test frameworks](test-frameworks.md)

@@@

1. @ref:[SBT Tasks](sbt-tasks.md)
1. @ref:[SBT Commands](sbt-commands.md)
1. @ref:[Test frameworks](test-frameworks.md)

## Tasks vs Commands
All Scala-Pact versions prior to 2.2.0 only had SBT Commands available.

The original motivation for commands was safety, they were designed to be really hard to misuse. For example, the consumer testing process works like this:

1. For safety, `sbt clean` to remove any lingering artifacts.
2. `sbt test` is the completely ordinary unit testing command, but it now has a side effect of generating pact contract files.

Note that one contract file is created per test at this point, meaning you can have multiple contract files for the *same* consumer <-> provider pair. This allows ScalaTest to run tests in parallel without us having to worry about execution order. However, the Pact standard is that all of these interactions live in a single file.

Enter a new task!

3. `sbt pactPack` simply takes all of the generated files and squashes them into one file per consumer <-> provider relationship.

Simple enough, but what if you forget to run `clean` initially? How will `pactPack` know NOT to include out of date pact artefacts? Short answer: It won't!

The Tasks are composable entities that can be easily combined with other tasks to create a chain of operations but they do NOT guarantee correctness.

The Commands do guarantee correctness because they force the full process to be adhered to, but they do so at the cost of time and the possibility of duplicate effort. For example: In a typical fail fast CI pipeline you will run `sbt clean update compile test` in sequence so that you can bail out and error at the first sign of trouble. If you extend that to `sbt clean update compile test pact-test` then you are effectively doing `sbt clean update compile test clean compile test pactPack`!

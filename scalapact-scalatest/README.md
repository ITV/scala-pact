# ScalaPact ScalaTest Library

## Setup
The ScalaPact ScalaTest Library is not a published resource yet. For the time being, we recommend that you checkout the project and do a local SBT publish (`sbt publish-local`) in order to use it as a dependency. Alternatively you can compile the JAR and place it manually in the unmanaged lib folder of your project.

If you decide to do a local publish, you will then need to add the dependency to your build.sbt file like this:

```
libraryDependencies ++= Seq(
  "com.itv" %% "scalapact" % "0.0.1-SNAPSHOT" % "test"
)
```

## Basic usage examples
There is an example test spec that can be found [here](https://github.com/ITV/ScalaPact/blob/master/src/test/scala/com/itv/scalapact/ExampleSpec.scala). The hope is that this will be a living example spec.

The pact files will be generated when you run `sbt test` and be written to the `target/pacts/` directory.

However, we recommend you install the sbt plugin and run `sbt pact` instead as this will give you cleaner pact files.

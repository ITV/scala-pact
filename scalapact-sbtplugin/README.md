# ScalaPact SBT Plugin

## Setup
The ScalaPact SBT plugin is not a published resource yet. For the time being, we recommend that you checkout the project and do a local SBT publish (`sbt publish-local`) in order to use it as a dependency.

You will then need to add the plugin dependency to your `project/plugins.sbt` file like this:

```
addSbtPlugin("com.itv.plugins" % "scalapact-plugin" % "0.1.0-SNAPSHOT")
```

## Basic usage examples
Once you have created some CDC tests using the ScalaPact lib, you can then run `sbt pact` from the command line to generate nicely compiled Pact files to your `target/pacts/` directory.

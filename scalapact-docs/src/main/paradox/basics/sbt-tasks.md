# SBT Tasks

Scala-Pact tasks are composable pact operations that are light in execution. Tasks require more care than commands since they do not guarantee correctness on the grounds that they assume all previous build artefacts are up to date and accurate.

## Configuration
Commands pick up their configuration from the plugin's sbt settings only.

## pactPack
Behaves like `pactTest` but only performs the final step of squashing pact files together.

See @ref:[SBT Commands](sbt-commands.md) for more details.

## pactStub
Behaves like `pactStubber` but does not regenerate contract files, it simply runs based on whatever it finds in the pact directory.

See @ref:[SBT Commands](sbt-commands.md) for more details.

## pactPush
Behaves like `pactPublish` but does not regenerate contract files, it simply pushes / publishes whatever contract files it finds in the pact directory.

See @ref:[SBT Commands](sbt-commands.md) for more details.

## pactCheck
Behaves like `pactVerify`.

See @ref:[SBT Commands](sbt-commands.md) for more details.

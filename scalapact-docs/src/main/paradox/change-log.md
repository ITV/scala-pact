# Change log
All notable changes and releases to this project will be documented here.

With thanks to all contributors!

- [itsDanOades]
- [smithleej]
- [Iulian-Antohe]
- [blake-boesinger]
- [agustafson]
- [raoulk]
- [BenParker22]
- [jtsmith0107]
- [yasuba]
- [phil-rice]
- [fergusstrange]
- [cjwebb]
- [benivf]
- [afiore]
- [pgordon9]
- [gcvt]
- [dandxy89]
- [orium]
- [opbokel]
- [garraspin]
- [Paulymorph]
- [jnatten]
- [randbw]
- [ShahOdin]
- [aggenebbisj]
- [mcarolan]
- [cmcmteixeira]
- [chrischivers]
- [jacemale]
- [dantb]
- [razerno]
- [jatcwang]
- ...your name here? :-)

Maintained by [davesmith00000].

## 2.3.12 - 2019-10-16
- Exit with error if any pact push operations fail (not API breaking, but potentially CI breaking) ([dantb])

## 2.3.11 - 2019-10-04
- Added support for Circe 0.12.1 ([agustafson], [jatcwang])

## 2.3.9 - 2019-06-27

- Add pact metadata to generated contracts (Fixes [#84](https://github.com/ITV/scala-pact/issues/84)) ([davesmith00000])
- Support finalising a `ScalaPactDescription` within the `ScalaPactForger` DSL (Fixes [#134](https://github.com/ITV/scala-pact/issues/134)) ([razerno])
- Refactored squashPactFiles method in SBT plugin ([dantb])
- Added support for token based Pact Broker authentication (Fixes [#140](https://github.com/ITV/scala-pact/issues/140)) ([dantb])

## 2.3.8 - 2019-04-26
- Added support for Http4s 0.20.0 ([jacemale])

## 2.3.6 - 2019-04-13
- Bugfix: Use correct `Headers` apply function in `Http4sRequestResponseFactory` ([davesmith00000])
- Use recommended scalafix SBT JVM memory settings ([davesmith00000])
- Added Http4s 0.20.0-M4 support ([chrischivers])
- Fix handling of URL parameter lists in `PactStubService` ([cmcmteixeira])

## 2.3.5 - 2019-02-15
- Added support for Circe 0.11 ([mcarolan])
- Fix typo in `ScalaPactVerify` ([aggenebbisj])
- Documentation corrections ([ShahOdin])
- Remove milestone version from package name ([randbw])

## 2.3.4 - 2018-12-31

- Adds support for http basic authentication (Fixes [#110](https://github.com/ITV/scala-pact/issues/110))  ([jnatten])
- Adds support for tagging pacts while publishing and verifying (Fixes [#107](https://github.com/ITV/scala-pact/issues/107))  ([jnatten])
- Added Http4s 0.20 support (version 0.20.0-M4) ([randbw])
- Fixed ScalaPactMatchingRuleArrayMinLength writing (Fixes [#108](https://github.com/ITV/scala-pact/issues/108)) ([Paulymorph])
- Fix spelling of ColourOutput object ([randbw])
- Circe upgraded to 0.10.1 ([dandxy89])

## 2.3.3 - 2018-10-24

- Added ability publish verification results (Fixes [#71](https://github.com/ITV/scala-pact/issues/71)) ([garraspin])
- Added support for Circe 0.10 ([dandxy89])

## 2.3.2 - 2018-08-12

- Added missing HTTP methods ([opbokel])
- Added ability to modify requests during Provider State setup ([garraspin])
- Failed Pact Broker calls now fail spectacularly (rather than silently)
- Pact verifications that perform no tests exit with non-zero code

## 2.3.1 - 2018-07-05

- Fixed find free port race condition ([orium])

## 2.3.0-RC1 - 2018-05-03

- Static dependencies converted to Typeclasses
- SBT 1.x support
- Verification test summary ([dandxy89])
- Documentation corrections ([pgordon9])
- Exclude logback.xml and log4j.properties from published jar ([cjwebb])
- Documentation corrections ([cjwebb])
- Bugfix: http client in 0-18 ignoring clientTimeout ([gcvt])

## 2.2.3 - 2018-02-19

- Update to stable Http4s 0.18 ([benivf])
- BUGFIX: Http4s interface incompatibility issue corrected ([afiore])
- Work to improve pact logging levels ([phil-rice])

## 2.2.2 - 2018-01-17

- SSL support added! ([phil-rice])
- Http4s 0.18.0-M8 milestone release support added ([benivf], [afiore])
- Circe 0.9 support added
- BUGFIX: Nasty lingering argonaut hard dependency has been removed.

## 2.2.1 - 2017-10-14

- BUGFIX: Pact publishing works again
- BUGFIX: Client timeout is supplied in seconds but is a duration after that.
- Added more compiler flags and wart remover
- Publisher will not publish files ending in tmp.json (protection for pactPush task)
- Verification failures now show the description for the interaction that failed
- Finding closest match now done by a points system

## 2.2.0 - 2017-10-05

**Breaking changes.**

With thanks to [yasuba] and [itsDanOades] for assisting with this release.

Highlights:

- Accepts basic auth for Pact Broker addresses ([phil-rice])
- Output dir (target/pacts) is now configurable via SBT config and environment variable "pact.rootDir" ([fergusstrange])
- Publish to multiple provider pact brokers
- **[BREAKING]** Matching logic completely rewritten (this could break existing tests in one or two cases, but once fixed they will be correct by the Pact Specification)
- **[BREAKING]** No more dependency conflicts
    - No external lib dependancies in any of the core libraries.
    - You must tell Scala-Pact which Http and JSON libraries to use (see setup guide).
    - JSON support via either Argonaut 6.1, Argonaut 6.2, or Circe 0.8.0
    - HTTP support via Http4s versions 0.15.0a, 0.16.2, 0.16.2a, or 0.17.0
    - Example projects use a range of Scala and lib combinations to prove they work (to be expanded)
- **[BREAKING]** Renamed sbt plugin to sbt-scalapact
- Composable SBT Tasks (pactPack, packPush, pactStub, and pactCheck)
- Scala-Pact env config (e.g. port and local dir) is now defined in the build sbt file.
  - Commands inherit and can override via the usual command line arguments.

Bug fixes:

- Scala-Pact can now be used reliably inside SBT to avoid constantly starting SBT from cold.
- Issues with matching rules and how they deal with deep or multiple layers of wildcards have been resolved.
- Regex matching of values no longer expects an exact match, instead expects to find at least one match.

Other improvements for contributors:

- Converted to multi-project build
- Many more modules, improved separation of concerns
  - Extending support for alternate HTTP and JSON frameworks is now easy.
- Better build / test scripts include testing cross compilation
- Simpler project version management
- Release script improvements

## 2.1.3 - 2017-03-24

- Convert SBT plugin to AutoPlugin ([yasuba])
- Ability to set a client timeout on verification ([itsDanOades])
- XML matching rules now work as expected
- Pact match failures against the stubber now return a 598 status, NOT a misleading 404.
- BUGFIX: Pact stubber now accepts chunked requests
- BUGFIX: Pact broker addresses can now include a port number
- BUGFIX: Bad test case fixed that would previously always succeed ([yasuba])

## 2.1.2 - 2017-01-25

- BUGFIX: Small issue that slipped through testing: In order for providerStates to work you had to declare a providerStateMatcher somewhere even if you already have a normal providerState setting. This has been resolved.

## 2.1.1 - 2017-01-24

- Accept `provider_state` on Pact file read to support old Pact formats ([itsDanOades])
- Added new `providerStateMatcher` partial function as a slicker, more useful alternative to the old mechanism. ([jtsmith0107])

## 2.1.0 - 2016-12-16

- Scala 2.12 release
    - Depends on http4s 0.15.0a which is bundled with argonaut 6.2-RC1 and scalaz 7.2.7
- Fixed intermittent bug that failed otherwise good tests
- Improved test speed
- Fixed issue where having a large number of pact tests in one suite caused a failure (with thanks to [BenParker22])
- Increased performance / capacity of pact stubber
- Provider state failures now fail noisily
- runConsumerTest returns a result instead of Unit ([agustafson])

## 2.0.0 - 2016-11-01

- Full Pact Specification Version 2.0 Compliance
- Body matching rules
- Provider verification test framework
- Strict mode
- Reduced library dependencies
- Improved mock config object
- Handle lowercase method during verify ([smithleej])

## 1.0.2 - 2016-10-04

- Remove logging implementation dependencies ([agustafson])
- Added example projects
- Scala-PactContractWriter creates directories 'target' and 'pacts' if they don't exist ([raoulk])
- Add preflight request operation (OPTIONS) ([itsDanOades])
- Codified testing process in a shell script.

## 1.0.1 - 2016-07-19

- Verifier does not post data if body is empty
- Added the ability to specify a pact version when verifying ([itsDanOades])

## 1.0.0 - 2016-05-26

- Initial public release following a number of in house only dev and milestone versions. ([davesmith00000], [Iulian-Antohe], [smithleej], [blake-boesinger])

[davesmith00000]: https://github.com/davesmith00000
[itsDanOades]: https://github.com/itsDanOades
[smithleej]: https://github.com/smithleej
[Iulian-Antohe]: https://github.com/iulian-antohe
[blake-boesinger]: https://github.com/blake-boesinger
[agustafson]: https://github.com/agustafson
[raoulk]: https://github.com/raoulk
[BenParker22]: https://github.com/BenParker22
[jtsmith0107]: https://github.com/jtsmith0107
[yasuba]: https://github.com/yasuba
[phil-rice]: https://github.com/phil-rice
[fergusstrange]: https://github.com/fergusstrange
[cjwebb]: https://github.com/cjwebb
[benivf]: https://github.com/benivf
[afiore]: https://github.com/afiore
[pgordon9]: https://github.com/pgordon9
[gcvt]: https://github.com/gcvt
[dandxy89]: https://github.com/dandxy89
[orium]: https://github.com/orium
[opbokel]: https://github.com/opbokel
[garraspin]: https://github.com/garraspin
[Paulymorph]: https://github.com/Paulymorph
[jnatten]: https://github.com/jnatten
[randbw]: https://github.com/randbw
[ShahOdin]: https://github.com/ShahOdin
[aggenebbisj]: https://github.com/aggenebbisj
[mcarolan]: https://github.com/mcarolan
[cmcmteixeira]: https://github.com/cmcmteixeira
[chrischivers]: https://github.com/chrischivers
[jacemale]: https://github.com/jacemale
[dantb]: https://github.com/dantb
[razerno]: https://github.com/razerno
[jatcwang]: https://github.com/jatcwang

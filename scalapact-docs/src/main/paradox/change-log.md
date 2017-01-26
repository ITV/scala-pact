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
- ...your name here? :-)

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

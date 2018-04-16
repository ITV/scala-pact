#!/bin/bash

set -e

echo "Sanity checking"
echo "***************"

echo ">> Compile everything"
sbt clean update compile test:compile

echo ">> Test everything"
sbt shared/test
sbt http4s016a/test http4s017/test http4s018/test
sbt argonaut62/test circe08/test circe09/test
sbt core/test plugin/test framework/test standalone/test
sbt pactSpec/test
sbt testsWithDeps/test

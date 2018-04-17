#!/bin/bash

set -e

echo "Test Spec and Examples"
echo "**********************"

# At this point, all the local publishing (therefore: clean update compile test publishLocal) should be done.

sbt test:compile
sbt pactSpec/test
sbt testsWithDeps/test

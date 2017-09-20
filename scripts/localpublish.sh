#!/bin/bash

# This script simple makes sure you have all the artefacts published locally
# No guarantees are offered with the running of this script other than
# version alignment

set -e

echo "Locally published Scala-Pact"
echo "****************************"

sbt clean update compile

echo ""
echo ">>> Plugin"
sbt plugin/test
sbt plugin/publish-local

echo ""
echo ">>> Standalone Stubber"
sbt standalone/test
sbt standalone/assembly

echo ""
echo ">>> Test Framework (2.11)"
sbt framework_2_11/test
sbt framework_2_11/publishLocal

echo ""
echo ">>> Test Framework (2.12)"
sbt framework_2_12/test
sbt framework_2_12/publishLocal

#!/bin/bash

# This script simple makes sure you have all the artefacts published locally
# No guarantees are offered with the running of this script other than
# version alignment

set -e

echo "Locally test Scala-Pact"
echo "****************************"

sbt clean update compile

sleep 1
echo ""
echo ">>> Core (2.10)"
sbt core_2_10/test

sleep 1
echo ""
echo ">>> Core (2.11)"
sbt core_2_11/test

sleep 1
echo ""
echo ">>> Core (2.12)"
sbt core_2_12/test

sleep 1
echo ""
echo ">>> Plugin"
sbt plugin/test

sleep 1
echo ""
echo ">>> Standalone Stubber"
sbt standalone/test

sleep 1
echo ""
echo ">>> Test Framework (2.11)"
sbt framework_2_11/test

sleep 1
echo ""
echo ">>> Test Framework (2.12)"
sbt framework_2_12/test

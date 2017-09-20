#!/bin/bash

# This script simple makes sure you have all the artefacts published locally
# No guarantees are offered with the running of this script other than
# version alignment

set -e

echo "Locally published Scala-Pact"
echo "****************************"

sbt clean update compile

sleep 1
echo ""
echo ">>> Core (2.10)"
sbt core_2_10/test
sbt core_2_10/publish-local

sleep 1
echo ""
echo ">>> Core (2.11)"
sbt core_2_11/test
sbt core_2_11/publish-local

sleep 1
echo ""
echo ">>> Core (2.12)"
sbt core_2_12/test
sbt core_2_12/publish-local

sleep 1
echo ""
echo ">>> Plugin"
sbt plugin/test
sbt plugin/publish-local

sleep 1
echo ""
echo ">>> Standalone Stubber"
sbt standalone/test
sbt standalone/assembly

sleep 1
echo ""
echo ">>> Test Framework (2.11)"
sbt framework_2_11/test
sbt framework_2_11/publishLocal

sleep 1
echo ""
echo ">>> Test Framework (2.12)"
sbt framework_2_12/test
sbt framework_2_12/publishLocal

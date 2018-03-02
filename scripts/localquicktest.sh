#!/bin/bash

# This script simple makes sure you have all the artefacts published locally
# No guarantees are offered with the running of this script other than
# version alignment

set -e

echo "Locally published Scala-Pact"
echo "****************************"

echo ""
echo ">>> Shared (2.12)"
sbt shared_2_12/clean shared_2_12/update shared_2_12/compile shared_2_12/test

echo ""
echo ">>> Argonaut 6.2 (2.12)"
sbt argonaut62_2_12/clean argonaut62_2_12/update argonaut62_2_12/compile argonaut62_2_12/test

echo ""
echo ">>> Core (2.12)"
sbt core_2_12/clean core_2_12/update core_2_12/compile core_2_12/test

echo ""
echo ">>> Pact Spec Tests 6.2 (2.12)"
sbt pactSpec_2_12/clean pactSpec_2_12/update pactSpec_2_12/compile pactSpec_2_12/test

echo ""l
echo ">>> Plugin"
sbt plugin/clean plugin/update plugin/compile plugin/test

echo ""
echo ">>> Standalone Stubber"
sbt standalone/clean standalone/update standalone/compile standalone/test

echo ""
echo ">>> Test Framework (2.12)"
sbt framework_2_12/clean framework_2_12/update framework_2_12/compile framework_2_12/test

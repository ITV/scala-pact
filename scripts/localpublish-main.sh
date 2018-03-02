#!/bin/bash

# This script simple makes sure you have all the artefacts published locally
# No guarantees are offered with the running of this script other than
# version alignment

set -e

echo "Locally published Scala-Pact (Main)"
echo "****************************"

function crossPublishLocal {
    NAME=$1
    sleep 1
    echo ""
    echo ">>> $NAME"
    sbt $NAME/clean $NAME/update $NAME/compile $NAME/test $NAME/publish-local  $NAME/publishM2
}

crossPublishLocal "core_2_10"
crossPublishLocal "core_2_11"
crossPublishLocal "core_2_12"
crossPublishLocal "plugin"
crossPublishLocal "standalone"
crossPublishLocal " scalapactStubber_2_10"
crossPublishLocal "scalapactStubber_2_11"
crossPublishLocal "scalapactStubber_2_12"
crossPublishLocal "framework_2_11"
crossPublishLocal "framework_2_12"

#!/bin/bash

# This script simple makes sure you have all the artefacts published locally
# No guarantees are offered with the running of this script other than
# version alignment

set -e

echo "REAL Publish Scala-Pact (Main)"
echo "******************************"

function crossPublishReal {
    NAME=$1
    sleep 1
    echo ""
    echo ">>> $NAME"
    sbt $NAME/clean $NAME/update $NAME/compile $NAME/test $NAME/publishSigned sonatypeRelease
}

crossPublishReal "core_2_10"
crossPublishReal "core_2_11"
crossPublishReal "core_2_12"
crossPublishReal "plugin"
crossPublishReal "framework_2_11"
crossPublishReal "framework_2_12"
crossPublishLocal "complexStubber"

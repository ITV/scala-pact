#!/bin/bash

# This script simple makes sure you have all the artefacts published locally
# No guarantees are offered with the running of this script other than
# version alignment

set -e

echo "Locally published Scala-Pact (Main)"
echo "****************************"

function publishLocally {
    NAME=$1
    sleep 1
    echo ""
    echo ">>> $NAME"
    sbt $NAME/clean $NAME/update $NAME/compile $NAME/test $NAME/publishLocal
}

publishLocally "core"
publishLocally "plugin"
publishLocally "standalone"
publishLocally "framework"

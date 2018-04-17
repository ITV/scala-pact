#!/bin/bash

set -e

echo "REAL Publish Scala-Pact (Main)"
echo "******************************"

function publishReal {
    NAME=$1
    sleep 1
    echo ""
    echo ">>> $NAME"
    sbt $NAME/clean $NAME/update $NAME/compile $NAME/test $NAME/publish
}

publishReal "core"
publishReal "plugin"
publishReal "standalone"
publishReal "framework"

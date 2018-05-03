#!/bin/bash

set -e

echo "REAL Publish Scala-Pact (Main)"
echo "******************************"

function publishReal {
    NAME=$1
    sleep 1
    echo ""
    echo ">>> $NAME"
    sbt $NAME/clean $NAME/update $NAME/compile $NAME/test $NAME/publishSigned sonatypeRelease
}

publishReal "core"
publishReal "pluginShared"
publishReal "plugin"
publishReal "pluginNoDeps"
#publishReal "standalone" # <-- we don't release this at the moment!
publishReal "framework"

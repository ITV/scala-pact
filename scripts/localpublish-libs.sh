#!/bin/bash

# This script simple makes sure you have all the artefacts published locally
# No guarantees are offered with the running of this script other than
# version alignment

set -e

echo "Local publish libs"
echo "******************"

function publishLocally {
    NAME=$1
    sbt $NAME/clean $NAME/update $NAME/compile $NAME/test $NAME/publishLocal
}

echo ""
echo ">>> Shared"
publishLocally "shared"

echo ""
echo ">>> Http4s 0.16.xa"
publishLocally "http4s016a"

echo ""
echo ">>> Http4s 0.17.x"
publishLocally "http4s017"

echo ""
echo ">>> Http4s 0.18.x"
publishLocally "http4s018"

echo ""
echo ">>> Argonaut 6.2"
publishLocally "argonaut62"

echo ""
echo ">>> Circe 0.8.x"
publishLocally "circe08"

echo ""
echo ">>> Circe 0.9.x"
publishLocally "circe09"

echo ""
echo ">>> Circe 0.10.x"
publishLocally "circe10"

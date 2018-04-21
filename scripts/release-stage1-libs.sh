#!/bin/bash

set -e

echo "REAL Publish libs"
echo "*****************"

function publishReal {
    NAME=$1
    sbt $NAME/clean $NAME/update $NAME/compile $NAME/test $NAME/publish
}

echo ""
echo ">>> Shared"
publishReal "shared"

echo ""
echo ">>> Http4s 0.16.xa"
publishReal "http4s016a"

echo ""
echo ">>> Http4s 0.17.x"
publishReal "http4s017"

echo ""
echo ">>> Http4s 0.18.x"
publishReal "http4s018"

echo ""
echo ">>> Argonaut 6.2"
publishReal "argonaut62"

echo ""
echo ">>> Circe 0.8.x"
publishReal "circe08"

echo ""
echo ">>> Circe 0.9.x"
publishReal "circe09"

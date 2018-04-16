#!/bin/bash

# This script simple makes sure you have all the artefacts published locally
# No guarantees are offered with the running of this script other than
# version alignment

set -e

echo "Local publish libs"
echo "******************"

function crossPublishLocal {
    NAME=$1
    sbt $NAME/clean $NAME/update +$NAME/compile +$NAME/test +$NAME/publishLocal
}

echo ""
echo ">>> Shared"
crossPublishLocal "shared"

echo ""
echo ">>> Http4s 0.16.xa"
crossPublishLocal "http4s016a"

echo ""
echo ">>> Http4s 0.17.x"
crossPublishLocal "http4s017"

echo ""
echo ">>> Http4s 0.18.x"
crossPublishLocal "http4s018"

echo ""
echo ">>> Argonaut 6.2"
crossPublishLocal "argonaut62"

echo ""
echo ">>> Circe 0.8.x"
crossPublishLocal "circe08"

echo ""
echo ">>> Circe 0.9.x"
crossPublishLocal "circe09"

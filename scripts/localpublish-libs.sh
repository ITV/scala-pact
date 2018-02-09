#!/bin/bash

# This script simple makes sure you have all the artefacts published locally
# No guarantees are offered with the running of this script other than
# version alignment

set -e

echo "Local publish libs"
echo "******************"

function crossPublishLocal {
    NAME=$1
    sbt $NAME/clean $NAME/update $NAME/compile $NAME/test $NAME/publish-local $NAME/publishM2
}

echo ""
echo ">>> Shared (2.10)"
crossPublishLocal "shared_2_10"

echo ""
echo ">>> Shared (2.11)"
crossPublishLocal "shared_2_11"

echo ""
echo ">>> Shared (2.12)"
crossPublishLocal "shared_2_12"

echo ""
echo ">>> Http4s 0.15.0a (2.10)"
crossPublishLocal "http4s0150a_2_10"

echo ""
echo ">>> Http4s 0.15.0a (2.11)"
crossPublishLocal "http4s0150a_2_11"

echo ""
echo ">>> Http4s 0.15.0a (2.12)"
crossPublishLocal "http4s0150a_2_12"

echo ""
echo ">>> Http4s 0.16.2a (2.10)"
crossPublishLocal "http4s0162a_2_10"

echo ""
echo ">>> Http4s 0.16.2a (2.11)"
crossPublishLocal "http4s0162a_2_11"

echo ""
echo ">>> Http4s 0.16.2a (2.12)"
crossPublishLocal "http4s0162a_2_12"

echo ""
echo ">>> Http4s 0.16.2 (2.10)"
crossPublishLocal "http4s0162_2_10"

echo ""
echo ">>> Http4s 0.16.2 (2.11)"
crossPublishLocal "http4s0162_2_11"

echo ""
echo ">>> Http4s 0.16.2 (2.12)"
crossPublishLocal "http4s0162_2_12"

#No such thing
##############
#echo ""
#echo ">>> Http4s 0.17.0 (2.10)"
#crossPublishLocal "http4s0170_2_10"
##############

echo ""
echo ">>> Http4s 0.17.0 (2.11)"
crossPublishLocal "http4s0170_2_11"

echo ""
echo ">>> Http4s 0.17.0 (2.12)"
crossPublishLocal "http4s0170_2_12"

echo ""
echo ">>> Http4s 0.18.0 (2.11)"
crossPublishLocal "http4s0180_2_11"

echo ""
echo ">>> Http4s 0.18.0 (2.12)"
crossPublishLocal "http4s0180_2_12"

echo ""
echo ">>> Argonaut 6.1 (2.10)"
crossPublishLocal "argonaut61_2_10"

echo ""
echo ">>> Argonaut 6.1 (2.11)"
crossPublishLocal "argonaut61_2_11"

#No such thing
##############
#echo ""
#echo ">>> Argonaut 6.1 (2.12)"
#crossPublishLocal "argonaut61_2_12"
##############

echo ""
echo ">>> Argonaut 6.2 (2.10)"
crossPublishLocal "argonaut62_2_10"

echo ""
echo ">>> Argonaut 6.2 (2.11)"
crossPublishLocal "argonaut62_2_11"

echo ""
echo ">>> Argonaut 6.2 (2.12)"
crossPublishLocal "argonaut62_2_12"

echo ""
echo ">>> Circe 0.8.0 (2.10)"
crossPublishLocal "circe08_2_10"

echo ""
echo ">>> Circe 0.8.0 (2.11)"
crossPublishLocal "circe08_2_11"

echo ""
echo ">>> Circe 0.8.0 (2.12)"
crossPublishLocal "circe08_2_12"

echo ""
echo ">>> Circe 0.9.0 (2.11)"
crossPublishLocal "circe09_2_11"

echo ""
echo ">>> Circe 0.9.0 (2.12)"
crossPublishLocal "circe09_2_12"

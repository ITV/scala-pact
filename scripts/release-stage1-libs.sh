#!/bin/bash

# This script simple makes sure you have all the artefacts published locally
# No guarantees are offered with the running of this script other than
# version alignment

set -e

echo "REAL Publish libs"
echo "*****************"

function crossPublishReal {
    NAME=$1
    sbt $NAME/clean $NAME/update $NAME/compile $NAME/test $NAME/publishSigned sonatypeRelease
}

echo ""
echo ">>> Shared (2.10)"
crossPublishReal "shared_2_10"

echo ""
echo ">>> Shared (2.11)"
crossPublishReal "shared_2_11"

echo ""
echo ">>> Shared (2.12)"
crossPublishReal "shared_2_12"



echo ""
echo ">>> Http4s 0.15.0a (2.10)"
crossPublishReal "http4s0150a_2_10"

echo ""
echo ">>> Http4s 0.15.0a (2.11)"
crossPublishReal "http4s0150a_2_11"

echo ""
echo ">>> Http4s 0.15.0a (2.12)"
crossPublishReal "http4s0150a_2_12"

echo ""
echo ">>> Http4s 0.16.2a (2.10)"
crossPublishReal "http4s0162a_2_10"

echo ""
echo ">>> Http4s 0.16.2a (2.11)"
crossPublishReal "http4s0162a_2_11"

echo ""
echo ">>> Http4s 0.16.2a (2.12)"
crossPublishReal "http4s0162a_2_12"

echo ""
echo ">>> Http4s 0.16.2 (2.10)"
crossPublishReal "http4s0162_2_10"

echo ""
echo ">>> Http4s 0.16.2 (2.11)"
crossPublishReal "http4s0162_2_11"

echo ""
echo ">>> Http4s 0.16.2 (2.12)"
crossPublishReal "http4s0162_2_12"

#No such thing
##############
#echo ""
#echo ">>> Http4s 0.17.0 (2.10)"
#crossPublishReal "http4s0170_2_10"
##############

echo ""
echo ">>> Http4s 0.17.0 (2.11)"
crossPublishReal "http4s0170_2_11"

echo ""
echo ">>> Http4s 0.17.0 (2.12)"
crossPublishReal "http4s0170_2_12"

echo ""
echo ">>> Http4s 0.18.0 (2.11)"
crossPublishReal "http4s0180_2_11"

echo ""
echo ">>> Http4s 0.18.0 (2.12)"
crossPublishReal "http4s0180_2_12"

echo ""
echo ">>> Argonaut 6.1 (2.10)"
crossPublishReal "argonaut61_2_10"

echo ""
echo ">>> Argonaut 6.1 (2.11)"
crossPublishReal "argonaut61_2_11"

#No such thing
##############
#echo ""
#echo ">>> Argonaut 6.1 (2.12)"
#crossPublishReal "argonaut61_2_12"
##############

echo ""
echo ">>> Argonaut 6.2 (2.10)"
crossPublishReal "argonaut62_2_10"

echo ""
echo ">>> Argonaut 6.2 (2.11)"
crossPublishReal "argonaut62_2_11"

echo ""
echo ">>> Argonaut 6.2 (2.12)"
crossPublishReal "argonaut62_2_12"

echo ""
echo ">>> Circe 0.8.0 (2.10)"
crossPublishReal "circe08_2_10"

echo ""
echo ">>> Circe 0.8.0 (2.11)"
crossPublishReal "circe08_2_11"

echo ""
echo ">>> Circe 0.8.0 (2.12)"
crossPublishReal "circe08_2_12"

echo ""
echo ">>> Circe 0.9.0 (2.11)"
crossPublishReal "circe09_2_11"

echo ""
echo ">>> Circe 0.9.0 (2.12)"
crossPublishReal "circe09_2_12"

#!/bin/bash

# This script simple makes sure you have all the artefacts published locally
# No guarantees are offered with the running of this script other than
# version alignment

set -e

echo "Local publish libs"
echo "******************"

function crossPublishLocal {
    NAME=$1
    sbt $NAME/clean $NAME/update $NAME/compile $NAME/test $NAME/publish-local
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
echo ">>> Argonaut 6.2 (2.10)"
crossPublishLocal "argonaut62_2_10"

echo ""
echo ">>> Argonaut 6.2 (2.11)"
crossPublishLocal "argonaut62_2_11"

echo ""
echo ">>> Argonaut 6.2 (2.12)"
crossPublishLocal "argonaut62_2_12"
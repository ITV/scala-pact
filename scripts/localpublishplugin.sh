#!/bin/bash

# This script simple makes sure you have all the artefacts published locally
# No guarantees are offered with the running of this script other than
# version alignment

set -e

echo "Locally published Scala-Pact-Plugin"
echo "***********************************"
set -e

function crossPublishLocal {
    NAME=$1
    sbt $NAME/clean $NAME/update $NAME/compile $NAME/publish-local  $NAME/publishM2
}
crossPublishLocal "scalapactStubber_2_12"
crossPublishLocal "scalapactStubber_2_11"
crossPublishLocal "scalapactStubber_2_10"
crossPublishLocal "plugin"
crossPublishLocal "standalone"
crossPublishLocal "core_2_10"
crossPublishLocal "shared_2_10"
crossPublishLocal "shared_2_11"
crossPublishLocal "shared_2_12"
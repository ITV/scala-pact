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
    sbt $NAME/clean $NAME/update $NAME/compile $NAME/publish-local
}
crossPublishLocal "complexStubber"
#crossPublishLocal "plugin"
#crossPublishLocal "standalone"
#crossPublishLocal "core_2_10"
#crossPublishLocal "shared_2_10"
#!/bin/bash

# This script simple makes sure you have all the artefacts published locally
# No guarantees are offered with the running of this script other than
# version alignment

set -e

echo "Locally published Scala-Pact"
echo "****************************"

bash scripts/localpublish-libs.sh

bash scripts/localpublish-main.sh

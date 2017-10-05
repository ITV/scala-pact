#!/bin/bash

set -e

# Codified release process
# This will only work with the correct global SBT and GPG setup

print_warning() {
    echo "An error was detected, this may require manual clean up before reattempting."
    exit 1
}

trap print_warning ERR

bash scripts/check-versions.sh

echo -e "Have you run the local tests and are you confident in the build? [y/n] \c"
read CONFIRM

if [ $CONFIRM != 'y' ]; then
  echo "Maybe you should do that first...(bash local-build-test.sh)"
  exit 1
else
  echo "Ok, here we go..."
fi

echo ""
echo "Starting Stage 1 Deploy"

bash release-stage1-libs.sh

echo ""
echo "Starting Stage 2 Deploy"

bash release-stage2-main.sh

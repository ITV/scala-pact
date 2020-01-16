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
echo "Preparing Scala-Pact for publishing"

sbt +prepareScalaPactPublish

echo ""
echo -e "Preparation complete, release to Sonatype? [y/n] \c"
read RELEASE_NOW

if [ $RELEASE_NOW != 'y' ]; then
  echo "Aborting as instructed."
  exit 1
else
  echo "Publishing now.."
fi

sbt +sonatypeBundleRelease

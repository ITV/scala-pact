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
echo ">>> Core"
cd scalapact-core
sbt +publishSigned
sbt sonatypeRelease
cd ..

echo ">>> SBT Plugin"
cd scalapact-sbtplugin
sbt publishSigned
sbt sonatypeRelease
cd ..

echo ">>> Test Framework"
cd scalapact-scalatest
sbt +publishSigned
sbt sonatypeRelease
cd ..

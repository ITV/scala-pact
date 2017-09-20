#!/bin/bash

set -e

# Codified release process
# This will only work with the correct global SBT and GPG setup

print_warning() {
    echo "An error was detected, this may require manual clean up before reattempting."
    exit 1
}

trap print_warning ERR

echo -e "Have you run the local tests and are you confident in the build? [y/n] \c"
read CONFIRM

if [ $CONFIRM != 'y' ]; then
  echo "Maybe you should do that first...(bash local-build-test.sh)"
  exit 1
else
  echo "Ok, here we go..."
fi

sbt clean update compile

echo ""
echo ">>> Core"
sbt core_2_10/test
sbt core_2_10/publishSigned
sbt core_2_10/sonatypeRelease
sbt core_2_11/test
sbt core_2_11/publishSigned
sbt core_2_11/sonatypeRelease
sbt core_2_12/test
sbt core_2_12/publishSigned
sbt core_2_12/sonatypeRelease

echo ""
echo ">>> Plugin"
sbt plugin/test
sbt plugin/publishSigned
sbt plugin/sonatypeRelease

echo ""
echo ">>> Test Framework (2.11)"
sbt framework_2_11/test
sbt framework_2_11/publishSigned
sbt framework_2_11/sonatypeRelease

echo ""
echo ">>> Test Framework (2.12)"
sbt framework_2_12/test
sbt framework_2_12/publishSigned
sbt framework_2_12/sonatypeRelease

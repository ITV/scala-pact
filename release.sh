#!/bin/bash

set -e

# Codified release process
# This will only work with the correct global SBT and GPG setup

bash check-versions.sh

echo -e "Have you run the local tests and are you confident in the build? [y/n] \c"
read CONFIRM

if [ $CONFIRM != 'y' ]; then
  echo "Maybe you should do that first...(bash local-build-test.sh)"
  exit 1
else
  echo "Ok, continuing..."
fi

echo ""
echo "Publishing"

echo ">>> Core"
cd scalapact-core
sbt +publishSigned
cd ..

echo ">>> SBT Plugin"
cd scalapact-sbtplugin
sbt publishSigned
cd ..

echo ">>> Test Framework"
cd scalapact-scalatest
sbt publishSigned
cd ..

echo ""
echo "Closing"

echo ">>> Core"
cd scalapact-core
sbt sonatypeClose
cd ..

echo ">>> SBT Plugin"
cd scalapact-sbtplugin
sbt sonatypeClose
cd ..

echo ">>> Test Framework"
cd scalapact-scalatest
sbt sonatypeClose
cd ..

echo ""
echo "Promoting"

echo ">>> Core"
cd scalapact-core
sbt sonatypePromote
cd ..

echo ">>> SBT Plugin"
cd scalapact-sbtplugin
sbt sonatypePromote
cd ..

echo ">>> Test Framework"
cd scalapact-scalatest
sbt sonatypePromote
cd ..

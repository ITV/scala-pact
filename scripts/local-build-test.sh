#!/bin/bash

# This script codifies the local testing process for the Scala-Pact project.
# It is pretty crude, it's just a time saving device.

set -e

cleanUpOnError() {
    echo "Error detected, cleaning up before exit"
    pkill -1 -f sbt-launch.jar
    exit 1
}

trap cleanUpOnError ERR

echo "Building and testing locally published Scala-Pact"
echo "*************************************************"

bash scripts/check-versions.sh

echo -e "Have you considered clearing out ~/.ivy2/local to ensure old artefacts aren't being picked up? [y/n] \c"
read CLEAR_LOCAL_CHECK

if [ $CLEAR_LOCAL_CHECK != 'y' ]; then
  echo "It's worth considering..."
  exit 1
else
  echo "Ok, proceeding..."
fi

echo ""
echo ">>> Core"
cd scalapact-core
sbt clean update compile
sbt test
sbt "+ publish-local"
cd ..

echo ""
echo ">>> Plugin"
cd scalapact-sbtplugin
sbt clean update compile
sbt test
sbt publish-local
cd ..

echo ""
echo ">>> Standalone Stubber"
cd scalapact-standalone-stubber
sbt clean update compile
sbt test
sbt assembly
cd ..

echo ""
echo ">>> Test Framework"
cd scalapact-scalatest
sbt clean update compile
sbt test
sbt "+ publish-local"
sbt pact-test
cd ..

echo ""
echo "Checking verifier..."
cd scalapact-scalatest
sbt "pact-stubber --port 1234" &

COUNTDOWN=30

echo "...giving the stubber a $COUNTDOWN second head start to warm up..."

while [ $COUNTDOWN -ne 0 ]
do
    echo "...$COUNTDOWN"
    COUNTDOWN=$(($COUNTDOWN - 1))
    sleep 1
done

echo "Verifying..."

sbt "pact-verify --source target/pacts"

pkill -1 -f sbt-launch.jar

cd ..

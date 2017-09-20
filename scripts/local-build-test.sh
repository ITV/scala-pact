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

echo -e "Have you considered clearing out ~/.ivy2/local to ensure old artefacts aren't being picked up? [y/n] \c"
read CLEAR_LOCAL_CHECK

if [ $CLEAR_LOCAL_CHECK != 'y' ]; then
  echo "It's worth considering... I'll leave you to think about it..."
  exit 1
else
  echo "Ok, proceeding..."
fi

bash localpublish.sh

echo ""
echo "Checking verifier..."
sbt "framework/pact-stubber --port 1234" &

COUNTDOWN=30

echo "...giving the stubber a $COUNTDOWN second head start to warm up..."

while [ $COUNTDOWN -ne 0 ]
do
    echo "...$COUNTDOWN"
    COUNTDOWN=$(($COUNTDOWN - 1))
    sleep 1
done

echo "Verifying..."

sbt "framework/pact-verify --source target/pacts"

pkill -1 -f sbt-launch.jar


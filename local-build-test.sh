#!/bin/bash

# This script codifies the local testing process for the Scala-Pact project.
# It is pretty crude, it's just a time saving device.

set -e

echo "Building and testing locally published Scala-Pact"
echo "*************************************"


CORE_VERSION=$(grep "version :=" scalapact-core/build.sbt | sed 's/version :=//g' | sed 's/ //g' | sed 's/,//g' | sed 's/\"//g')
PLUGIN_VERSION=$(grep "version :=" scalapact-sbtplugin/project/Build.scala | sed 's/version :=//g' | sed 's/ //g' | sed 's/,//g' | sed 's/\"//g')
STANDALONE_VERSION=$(grep "version :=" scalapact-standalone-stubber/build.sbt | sed 's/version :=//g' | sed 's/ //g' | sed 's/,//g' | sed 's/\"//g')
TEST_VERSION=$(grep "version :=" scalapact-scalatest/build.sbt | sed 's/version :=//g' | sed 's/ //g' | sed 's/,//g' | sed 's/\"//g')
TEST_PLUGIN_VERSION=$(grep "scalapact-plugin" scalapact-scalatest/project/plugins.sbt | awk '{print $5}' | sed -e 's/[:space:,\")]//g')

ALL_VERSIONS="$CORE_VERSION $PLUGIN_VERSION $STANDALONE_VERSION $TEST_VERSION $TEST_PLUGIN_VERSION"
ALL_EXPECTED="$CORE_VERSION $CORE_VERSION $CORE_VERSION $CORE_VERSION $CORE_VERSION"

if [[ $ALL_VERSIONS == $ALL_EXPECTED ]]; then
  echo "Version set to $CORE_VERSION, proceed? [y/n]"
  read CORRECT
else
  echo "Project versions did not match."
  echo "Have you aligned the versions in all the projects? Found:"
  echo "> core:                  $CORE_VERSION"
  echo "> plugin:                $PLUGIN_VERSION"
  echo "> standalone stubber:    $STANDALONE_VERSION"
  echo "> test framework:        $TEST_VERSION"
  echo "> test framework plugin: $TEST_PLUGIN_VERSION"
  echo "Exiting, please fix the problem by aligning the versions."
  exit 1
fi

if [ $CORRECT != 'y' ]; then
  echo "Exiting"
  exit 1
else
  echo "Continuing..."
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
sbt publish-local
sbt pact-test
cd ..

echo ""
echo "Checking verifier..."
cd scalapact-scalatest
sbt "pact-stubber --port 1234" &

COUNTDOWN=15

echo "...giving the stubber a $COUNTDOWN head start to warm up..."

while [ $COUNTDOWN -ne 0 ]
do
    echo "...$COUNTDOWN"
    COUNTDOWN=$(($COUNTDOWN - 1))
    sleep 1
done

sbt "pact-verify --source target/pacts"

pkill -1 -f sbt-launch.jar

cd ..

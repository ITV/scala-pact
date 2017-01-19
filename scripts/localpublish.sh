#!/bin/bash

# This script simple makes sure you have all the artefacts published locally
# No guarantees are offered with the running of this script other than
# version alignment

set -e

echo "Checking Scala-Pact Version Alignment"
echo "*************************************"

# Mostly copy pasted from check-version.sh but requires no confirmation to proceed
CORE_VERSION=$(grep "version :=" scalapact-core/build.sbt | sed 's/version :=//g' | sed 's/ //g' | sed 's/,//g' | sed 's/\"//g')
PLUGIN_VERSION=$(grep "version :=" scalapact-sbtplugin/project/Build.scala | sed 's/version :=//g' | sed 's/ //g' | sed 's/,//g' | sed 's/\"//g')
STANDALONE_VERSION=$(grep "version :=" scalapact-standalone-stubber/build.sbt | sed 's/version :=//g' | sed 's/ //g' | sed 's/,//g' | sed 's/\"//g')
TEST_VERSION=$(grep "version :=" scalapact-scalatest/build.sbt | sed 's/version :=//g' | sed 's/ //g' | sed 's/,//g' | sed 's/\"//g')
TEST_PLUGIN_VERSION=$(grep "scalapact-plugin" scalapact-scalatest/project/plugins.sbt | awk '{print $5}' | sed -e 's/[:space:,\")]//g')

ALL_VERSIONS="$CORE_VERSION $PLUGIN_VERSION $STANDALONE_VERSION $TEST_VERSION $TEST_PLUGIN_VERSION"
ALL_EXPECTED="$CORE_VERSION $CORE_VERSION $CORE_VERSION $CORE_VERSION $CORE_VERSION"

if [[ $ALL_VERSIONS == $ALL_EXPECTED ]]; then
  echo "Version set to $CORE_VERSION"
else
  echo "Project versions did not match."
  echo "> core:                  $CORE_VERSION"
  echo "> plugin:                $PLUGIN_VERSION"
  echo "> standalone stubber:    $STANDALONE_VERSION"
  echo "> test framework:        $TEST_VERSION"
  echo "> test framework plugin: $TEST_PLUGIN_VERSION"
  echo "Exiting, please fix the problem by aligning the versions."
  exit 1
fi

echo "Locally published Scala-Pact"
echo "****************************"

echo ""
echo ">>> Core"
cd scalapact-core
sbt clean update compile test +publishLocal
cd ..

echo ""
echo ">>> Plugin"
cd scalapact-sbtplugin
sbt clean update compile test publish-local
cd ..

echo ""
echo ">>> Standalone Stubber"
cd scalapact-standalone-stubber
sbt clean update compile test assembly
cd ..

echo ""
echo ">>> Test Framework"
cd scalapact-scalatest
sbt clean update compile test +publishLocal
cd ..

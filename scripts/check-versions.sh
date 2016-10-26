#!/bin/bash

set -e

echo "Checking Scala-Pact Version Alignment"
echo "*************************************"


CORE_VERSION=$(grep "version :=" scalapact-core/build.sbt | sed 's/version :=//g' | sed 's/ //g' | sed 's/,//g' | sed 's/\"//g')
PLUGIN_VERSION=$(grep "version :=" scalapact-sbtplugin/project/Build.scala | sed 's/version :=//g' | sed 's/ //g' | sed 's/,//g' | sed 's/\"//g')
STANDALONE_VERSION=$(grep "version :=" scalapact-standalone-stubber/build.sbt | sed 's/version :=//g' | sed 's/ //g' | sed 's/,//g' | sed 's/\"//g')
TEST_VERSION=$(grep "version :=" scalapact-scalatest/build.sbt | sed 's/version :=//g' | sed 's/ //g' | sed 's/,//g' | sed 's/\"//g')
TEST_PLUGIN_VERSION=$(grep "scalapact-plugin" scalapact-scalatest/project/plugins.sbt | awk '{print $5}' | sed -e 's/[:space:,\")]//g')
DOCS_VERSION=$(grep "version :=" scalapact-docs/build.sbt | sed 's/version :=//g' | sed 's/ //g' | sed 's/,//g' | sed 's/\"//g')

ALL_VERSIONS="$CORE_VERSION $PLUGIN_VERSION $STANDALONE_VERSION $TEST_VERSION $TEST_PLUGIN_VERSION $DOCS_VERSION"
ALL_EXPECTED="$CORE_VERSION $CORE_VERSION $CORE_VERSION $CORE_VERSION $CORE_VERSION $CORE_VERSION"

if [[ $ALL_VERSIONS == $ALL_EXPECTED ]]; then
  echo -e "Version set to $CORE_VERSION, proceed? [y/n] \c"
  read CORRECT
else
  echo "Project versions did not match."
  echo "Have you aligned the versions in all the projects? Found:"
  echo "> core:                  $CORE_VERSION"
  echo "> plugin:                $PLUGIN_VERSION"
  echo "> standalone stubber:    $STANDALONE_VERSION"
  echo "> test framework:        $TEST_VERSION"
  echo "> test framework plugin: $TEST_PLUGIN_VERSION"
  echo "> docs:                  $DOCS_VERSION"
  echo "Exiting, please fix the problem by aligning the versions."
  exit 1
fi

if [ $CORRECT != 'y' ]; then
  echo "Exiting"
  exit 1
else
  echo "Continuing..."
fi

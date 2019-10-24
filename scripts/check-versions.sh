#!/bin/bash

set -e

echo "Checking Scala-Pact Examples Version Alignment"
echo "**********************************************"

CORE_VERSION=$(grep "version :=" build.sbt | sed 's/version :=//g' | sed 's/ //g' | sed 's/,//g' | sed 's/\"//g')
CONSUMER_VERSION=$(grep "scalapact-scalatest" example/consumer/build.sbt | awk '{print $5}' | sed -e 's/[:space:,\")]//g')
CONSUMER_PLUGIN_VERSION=$(grep "sbt-scalapact" example/consumer/project/plugins.sbt | awk '{print $5}' | sed -e 's/[:space:,\")]//g')
PROVIDER_PLUGIN_VERSION=$(grep "sbt-scalapact" example/provider/project/plugins.sbt | awk '{print $5}' | sed -e 's/[:space:,\")]//g')
PROVIDER_TESTS_VERSION=$(grep "scalapact-scalatest" example/provider_tests/build.sbt | awk '{print $5}' | sed -e 's/[:space:,\")]//g')

ALL_VERSIONS="$CORE_VERSION $CONSUMER_VERSION $CONSUMER_PLUGIN_VERSION $PROVIDER_PLUGIN_VERSION $PROVIDER_TESTS_VERSION"
ALL_EXPECTED="$CORE_VERSION $CORE_VERSION $CORE_VERSION $CORE_VERSION $CORE_VERSION"

if [[ $ALL_VERSIONS == $ALL_EXPECTED ]]; then
  if [[ -z "${CI}" ]]; then
    echo -e "Version set to $CORE_VERSION, proceed? [y/n] \c"
    read CORRECT
  else
    CORRECT="y"
  fi
else
  echo "Project versions did not match."
  echo "Have you aligned the versions in all the projects? Found:"
  echo "> core:                  $CORE_VERSION (build.sbt)"
  echo "> consumer:              $CONSUMER_VERSION (example/consumer/build.sbt)"
  echo "> consumer plugin:       $CONSUMER_PLUGIN_VERSION (example/consumer/project/plugins.sbt)"
  echo "> provider:              (does not use the test suite!)"
  echo "> provider plugin:       $PROVIDER_PLUGIN_VERSION (example/provider/project/plugins.sbt)"
  echo "> provider_tests:        $PROVIDER_TESTS_VERSION (example/provider_tests/build.sbt)"
  echo "> provider_tests plugin: (does not use the plugin!)"
  echo "Exiting, please fix the problem by aligning the versions."
  exit 1
fi

if [ $CORRECT != 'y' ]; then
  echo "Exiting"
  exit 1
else
  echo "Continuing..."
fi
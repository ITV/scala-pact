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

function simple_countdown {
    COUNTDOWN=$1

    while [ $COUNTDOWN -ne 0 ]
    do
        echo "...$COUNTDOWN"
        COUNTDOWN=$(($COUNTDOWN - 1))
        sleep 1
    done
}


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

bash scripts/check-versions.sh

bash scripts/localpublish.sh

echo ""
echo "Checking verifier..."

CORE_VERSION=$(grep "version :=" build.sbt | sed 's/version :=//g' | sed 's/ //g' | sed 's/,//g' | sed 's/\"//g')

PLUGIN_FILE=project/pact-plugin.sbt
PACT_CONFIG_FILE=scalapact-scalatest/pact.sbt

function remove_plugin_file {
    if [ -f $PLUGIN_FILE ]; then rm $PLUGIN_FILE; fi
}
function remove_config_file {
    if [ -f $PACT_CONFIG_FILE ]; then rm $PACT_CONFIG_FILE; fi
}

remove_plugin_file
remove_config_file

echo "addSbtPlugin(\"com.itv\" % \"sbt-scalapact\" % \"$CORE_VERSION\")" > $PLUGIN_FILE

cat > $PACT_CONFIG_FILE <<EOL
import com.itv.scalapact.plugin.ScalaPactPlugin._

providerStates := Seq(
("Resource with ID 1234 exists", (_: String) => {
 println("Injecting key 1234 into the database...")
 // Do some work to ensure the system under test is
 // in an appropriate state before verification

 true
})
)

providerStateMatcher := {
case key if key == "Resource with ID 1234 exists" =>
 println("Injecting key 1234 into the database...")
 true
}

pactBrokerAddress := "http://localhost"
providerName := "Their Provider Service"
consumerNames := Seq("My Consumer")
pactContractVersion := "2.0.0"
allowSnapshotPublish := false
EOL

sbt "project framework_2_12" update
sbt "; project framework_2_12; pact-stubber --port 1234" &

echo "...giving the stubber a $COUNTDOWN second head start to warm up..."
simple_countdown 30

echo "Verifying..."

sbt "; project framework_2_12; pact-verify --source target/pacts"

pkill -1 -f sbt-launch.jar

remove_plugin_file
remove_config_file

echo "Checking example setups"
echo "***********************"

echo ""
echo "> Consumer tests"
cd example/consumer
sbt clean update compile pact-test
cd ..
bash deliver.sh

echo ""
echo "> Provider verification by test suite"
cd provider_tests
sbt clean update compile test
cd ..

echo ""
echo "> Provider verification by external testing"
cd provider
sbt run &

echo "..wait a bit for the service to start"
simple_countdown 10

sbt "pact-verify --source delivered_pacts/ --host localhost --port 8080 --clientTimeout 2"

cd ../..

pkill -1 -f sbt-launch.jar
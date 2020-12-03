#!/bin/bash

set -e

source scripts/test-header.sh

echo "Checking pending pacts tests"
echo "***********************"

cd pending-pact-tests/consumer
sbt clean update
sbt "pactPublish --clientTimeout 5"
cd ..

cd provider
sbt clean update test

echo "Testing using the CLI with --includeWipPactsSince"
sbt run &
echo "warming things up..."
simple_countdown 10
sbt "pactVerify --enablePending true --includeWipPactsSince 2020-11-11T00:42Z --host localhost --port 8080 --clientTimeout 5"
cd ../..

pkill -1 -f sbt-launch.jar
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
simple_countdown 30
sbt "pactVerify --enablePending true --includeWipPactsSince 2020-11-11T00:42Z --port 8080"
cd ../..

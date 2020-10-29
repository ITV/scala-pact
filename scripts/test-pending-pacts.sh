#!/bin/bash

set -e

echo "Checking pending pacts tests"
echo "***********************"

cd pending-pact-tests/consumer
sbt clean update
sbt "pactPublish --clientTimeout 5"
cd ..

cd provider
sbt clean update test
cd ../..

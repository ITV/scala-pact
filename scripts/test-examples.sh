#!/bin/bash

source scripts/test-header.sh

echo "Checking example setups"
echo "***********************"

echo ""
echo "> Consumer tests"
cd example/consumer
sbt +clean +update +compile
sbt "+pactPublish --clientTimeout 5"
cd ..
bash deliver.sh

echo ""
echo "> Provider verification by test suite"
cd provider_tests
sbt +clean +update +compile +test
cd ..

echo ""
echo "> Provider verification using pact broker and pacts-for-verification endpoint"
cd provider_pact-for-verification
sbt +clean +update +compile +test
cd ..

echo ""
echo "> Provider verification by external testing"
cd provider
sbt +run &

echo "..wait a bit for the service to start"
simple_countdown 30

sbt "+pactVerify --source delivered_pacts/ --host localhost --port 8080 --clientTimeout 2"

cd ../..

pkill -1 -f sbt-launch.jar

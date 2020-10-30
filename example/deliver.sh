#!/bin/bash

echo "Attempting to deliver pact file"

PACT_NAME="scala-pact-consumer_scala-pact-provider.json"
PACT_FILE="consumer/target/pacts/$PACT_NAME"

if [ ! -f $PACT_FILE ]
  then
    echo "Expected pact file did not exist: $PACT_FILE"
    echo "Have you run 'sbt pact-test' in the consumer project?"
    exit 1
fi

PROVIDER_STUBBED_PATH="provider"
PROVIDER_TESTS_PATH="provider_tests"

if [ ! -d $PROVIDER_STUBBED_PATH ]
  then
    echo "Missing provider project at: $PROVIDER_STUBBED_PATH"
    exit 1
fi

if [ ! -d $PROVIDER_TESTS_PATH ]
  then
    echo "Missing provider tests project at: $PROVIDER_TESTS_PATH"
    exit 1
fi

PATH_1=provider/delivered_pacts/$PACT_NAME
PATH_2=provider_tests/delivered_pacts/$PACT_NAME

if [ -f $PATH_1 ]; then rm $PATH_1; fi
if [ -f $PATH_2 ]; then rm $PATH_2; fi

mkdir -p provider/delivered_pacts
cp $PACT_FILE $PATH_1

mkdir -p provider_tests/delivered_pacts
cp $PACT_FILE $PATH_2

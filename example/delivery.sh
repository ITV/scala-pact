#!/bin/bash

echo "Attempting to deliver pact file"

PACT_NAME="Consumer_Provider.json"
PACT_FILE="consumer/target/pacts/$PACT_NAME"

if [ ! -f $PACT_FILE ]
  then
    echo "Expected pact file did not exist: $PACT_FILE"
    echo "Have you run 'sbt pact-test' in the consumer project?"
    exit 1
fi

mkdir -p provider/delivered_pacts
cp $PACT_FILE provider/delivered_pacts/$PACT_NAME

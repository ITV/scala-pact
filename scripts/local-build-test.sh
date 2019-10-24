#!/bin/bash

START_TIME=$(date +%s)
END_TIME=0
ELAPSED_TIME=0

source scripts/test-header.sh

echo "Building and testing locally published Scala-Pact"
echo "*************************************************"

if [[ -z "${CI}" ]]; then
  echo -e "Have you considered clearing out ~/.ivy2/local to ensure old artefacts aren't being picked up? [y/n] \c"
  read CLEAR_LOCAL_CHECK
else
  CLEAR_LOCAL_CHECK="y"
fi

if [ $CLEAR_LOCAL_CHECK != 'y' ]; then
  echo "It's worth considering... I'll leave you to think about it..."
  exit 1
else
  echo "Ok, proceeding..."
fi

bash scripts/check-versions.sh

bash scripts/localpublish.sh

echo "Taking a breather... (1)"
simple_countdown 5

bash scripts/spec-tests.sh

echo "Taking a breather... (2)"
simple_countdown 5

bash scripts/test-verifier.sh

echo "Taking a breather... (3)"
simple_countdown 5

bash scripts/test-examples.sh

END_TIME=$(date +%s)
ELAPSED_TIME=$(($END_TIME - $START_TIME))

echo "Build successful. Took $ELAPSED_TIME seconds."
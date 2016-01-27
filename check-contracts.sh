#!/bin/bash

# Initialisation
PROJECT_ROOT_DIR=$(pwd)
JSON_PACT_FILE_PATH="target/pacts/consumer-provider.json"
PORT=1234
BASE_URL="http://localhost:$PORT"

# Step 1
# Generate the pact files for the consumer and provider projects
sbt clean update test

# Step 2
# Start stub service with pact contracts
pact-mock-service start --port=$PORT &

sleep 1 # Give it a chance...

echo "Clear exisiting interactions"

curl -X DELETE -H "X-Pact-Mock-Service: true" localhost:1234/interactions

echo "Uploading interation"

INTERACTIONS=$(cat $JSON_PACT_FILE_PATH | jq '.interactions')
INTERACTION_COUNT=$(echo $INTERACTIONS | jq '. | length')

while [ "$INTERACTION_COUNT" -gt 0 ]
do
  INTERACTION_COUNT=$(($INTERACTION_COUNT - 1))
  $(echo $INTERACTIONS | jq --compact-output .[$INTERACTION_COUNT] > tmp.json)

  curl -X POST \
  -H "Content-Type: application/json" \
  -H "X-Pact-Mock-Service: true" \
  localhost:1234/interactions \
  -d @tmp.json

  rm tmp.json
done

echo ""

# Step 3
# Verify the PACT contracts against the stub service
cd $PROJECT_ROOT_DIR/pact-verifier
#bundle install
endpoint=$BASE_URL spec_url="$PROJECT_ROOT_DIR/$JSON_PACT_FILE_PATH" bundle exec rake pact:verify

# Kill the stub
cd $PROJECT_ROOT_DIR
echo "kill the UpstreamService stub"
pact-mock-service stop

#!/bin/bash

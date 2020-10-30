#!/bin/bash

# This script codifies the local testing process for the Scala-Pact project.
# It is pretty crude, it's just a time saving device.

set -e

PLUGIN_FILE=project/pact-plugin.sbt
PACT_CONFIG_FILE=tests-with-deps/pact.sbt

function remove_plugin_file {
    if [ -f $PLUGIN_FILE ]; then rm $PLUGIN_FILE; fi
}
function remove_config_file {
    if [ -f $PACT_CONFIG_FILE ]; then rm $PACT_CONFIG_FILE; fi
}

cleanUpOnError() {
    echo "Error detected, cleaning up before exit"

    remove_plugin_file
    remove_config_file

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
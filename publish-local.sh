#!/bin/bash

cd scalapact-core
../sbt clean update compile test "+ publish-local"

cd ../scalapact-sbtplugin
../sbt clean update compile test publish-local

cd ../scalapact-scalatest
../sbt clean update compile test publish-local

cd ..

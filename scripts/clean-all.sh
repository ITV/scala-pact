#!/usr/bin/env bash
bash scripts/check-versions.sh

echo ""
echo ">>> Core"
pushd scalapact-core
    sbt clean
popd

echo ""
echo ">>> Plugin"
pushd scalapact-sbtplugin
    sbt clean
popd

echo ""
echo ">>> Standalone Stubber"
pushd scalapact-standalone-stubber
    sbt clean
popd

echo ""
echo ">>> Test Framework"
pushd scalapact-scalatest
    sbt clean
popd


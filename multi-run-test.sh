#!/bin/bash
#
pushd "$(dirname "$0")"

source .jenkins/test.sh

for i in $(seq 1 $1);
do
    echo "Run: $i"
    echo "########"
    run_tests .
    testresult=$?
    mv ./target/test-reports/junit.xml  ./target/test-reports/$(date +%Y-%m-%d-%H:%M).xml
    pwd
    generate_report  ./target/test-reports ./target/rep/index.html
done

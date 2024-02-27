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
    mv ./target/test-reports/junit.xml  ./target/test-results/$(date +%Y-%m-%d-%H:%M).xml

#    generate_allure_report . ./target/test-reports
done

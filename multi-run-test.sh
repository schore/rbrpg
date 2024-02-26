#!/bin/bash
#
pushd "$(dirname "$0")"

source .jenkins/test.sh

for i in $(seq 1 $1);
do
    echo "Run: $i"
    echo "########"
    rm ./target/test-reports/*.xml
    run_tests .
    generate_allure_report . ./target/test-reports
done

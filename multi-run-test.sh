#!/bin/bash
#
pushd "$(dirname "$0")"

source .jenkins/test.sh

for i in {0..$1}
do
    rm ./target/test-reports/*.xml
    run_tests .
    generate_allure_report . ./target/test-reports
done

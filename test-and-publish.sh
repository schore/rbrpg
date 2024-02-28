#!/bin/bash
#
pushd "$(dirname "$0")"

source .jenkins/test.sh

run_tests .
testresult=$?

mv ./target/test-reports/junit.xml  ./target/test-reports/$(date +%Y-%m-%d-%H:%M).xml
generate_report  ./target/test-reports ./target/rep/index.html

if  [[ $testresult == 0 ]]
then
    echo "Publish to github pages"
    .jenkins/publish.sh
fi

popd

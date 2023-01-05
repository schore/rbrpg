#!/bin/bash
#
pushd "$(dirname "$0")"

source .jenkins/test.sh



run_tests .
testresult=$?
generate_allure_report . ./target/test-reports

if  [[ $testresult == 0 ]]
then
    echo "Publish to github pages"
    .jenkins/publish.sh
fi

popd

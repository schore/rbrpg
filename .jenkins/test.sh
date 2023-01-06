#!/bin/bash
#
#

run_tests () {
    pushd $1

    local result
    npx --call='lein test :all'
    result=$?
    popd
    return $result
}

generate_allure_report () {
    local execution_dir=$1
    local test_xunit_dir=$2
    local allure=../allure/allure-2.20.1/bin/allure

    pushd $1
    if [ -d $test_xunit_dir/allure-report/history ]
    then
        cp -r $test_xunit_dir/allure-report/history $test_xunit_dir
    fi

    $allure generate -c $test_xunit_dir -o $test_xunit_dir/allure-report
    popd
}

if [ $0 == ${BASH_SOURCE} ]
then
    run_tests "$(dirname "$0")/.."
    exit $?
fi


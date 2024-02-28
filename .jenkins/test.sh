#!/bin/bash
#
#

run_tests () {
    pushd $1

    local result
    npx --call='lein kaocha'
    result=$?
    popd
    return $result
}

generate_report () {
    local output_dir=$2
    local test_xunit_dir=$1

    junit2html $1/* --report-matrix $2
}

if [ $0 == ${BASH_SOURCE} ]
then
    run_tests "$(dirname "$0")/.."
    exit $?
fi


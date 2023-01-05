#!/bin/bash
#
#

run_tests () {
    pushd $1

    local result
    result= npx --call='lein test :all'
    popd
    return $result
}

if [ $0 == ${BASH_SOURCE} ]
then
    run_tests "$(dirname "$0")/.."
    exit $?
fi

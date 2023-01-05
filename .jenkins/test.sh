#!/bin/bash
#
#

run_tests () {
    cd $1
    npx --call='lein test :all'
    return $?
}

if [ $0 == ${BASH_SOURCE} ]
then
    run_tests "$(dirname "$0")/.."
fi

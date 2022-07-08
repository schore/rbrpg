#!/bin/bash -e
#
cd "$(dirname "$0")"

./test.sh
./publish.sh

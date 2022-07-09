#!/bin/bash -e
#
cd "$(dirname "$0")/.jenkins"

./test.sh
./publish.sh

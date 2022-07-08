#!/bin/bash -e

cd "$(dirname "$0")/.."

npx --call='lein test :all'

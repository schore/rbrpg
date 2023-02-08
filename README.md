# Role based RPG

[![Clojure CI](https://github.com/schore/rbrpg/actions/workflows/clojure.yml/badge.svg)](https://github.com/schore/rbrpg/actions/workflows/clojure.yml)

Just a small game, which you can play on github pages

[rbRPG](https://schore.github.io/rbrpg-static-site/)

## Prerequisites

You will need [Leiningen][1] 2.0 or above installed.

[1]: https://github.com/technomancy/leiningen

## Running
### Dev environment

`
npm install .
shadow-cljs watch app
`

### Run tests

`
npm install .
npx lein kaocha
`

### Run locally

`
npm install .
lein uberjar
java -jar target/
`

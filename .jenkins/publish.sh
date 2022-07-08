#!/bin/bash -e

cd "$(dirname "$0")/.."


rm -rf rbrpg-static-site
git clone git@github.com:schore/rbrpg-static-site.git

npx shadow-cljs release app
cp -f target/cljsbuild/public/js/app.js rbrpg-static-site/

cd rbrpg-static-site
git add app.js
git config user.name "Jenkins"
git config user.email "jenkisn@jenkins.com"
git commit --amend -m 'automatic update'

git push -f origin main
cd ..

rm -rf rbrpg-static-site

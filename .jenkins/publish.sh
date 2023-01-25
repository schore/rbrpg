#!/bin/bash -e

cd "$(dirname "$0")/.."

rm -rf rbrpg-static-site

if [[ $(git status --porcelain | wc -l) -gt 0 ]]; then
    echo "Commit before puplishing"
    exit -1
fi

git clone git@github.com:schore/rbrpg-static-site.git

npx shadow-cljs release app
cp -f resources/public/css/screen.css rbrpg-static-site/css/
cp -f resources/docs/docs.md rbrpg-static-site/
cp -f target/cljsbuild/public/js/app.js rbrpg-static-site/

cd rbrpg-static-site
git add .
git config user.name "Jenkins"
git config user.email "jenkisn@jenkins.com"
git commit --amend -m 'automatic update'

git push -f origin main
cd ..

rm -rf rbrpg-static-site

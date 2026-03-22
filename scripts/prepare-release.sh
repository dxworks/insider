#!/bin/bash
set -e

VERSION=$1

if [ -z "$VERSION" ]; then
  echo "Usage: $0 <version>"
  exit 1
fi

mkdir -p insider/results
cp README.md insider/README.md
cp build/libs/insider*.jar insider/insider.jar
cp bin/insider.sh insider/insider.sh
cp bin/insider.bat insider/insider.bat
chmod +x insider/insider.sh
cp languages.yml insider/languages.yml
cp -R config insider/config

zip -r insider.zip insider

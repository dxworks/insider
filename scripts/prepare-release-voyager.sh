#!/bin/bash
set -e

VERSION=$1

if [ -z "$VERSION" ]; then
  echo "Usage: $0 <version>"
  exit 1
fi

mkdir -p insider/results
mkdir -p insider/templates
cp README.md insider/README.md
cp build/libs/insider*.jar insider/insider.jar
cp bin/insider.sh insider/insider.sh
cp bin/insider.bat insider/insider.bat
chmod +x insider/insider.sh
cp instrument.yml insider/instrument.yml
cp instrument.v2.yml insider/instrument.v2.yml
cp languages.yml insider/languages.yml
cp -R config insider/config
cp lib/insider-summary.py insider/insider-summary.py
cp lib/summary_extract.py insider/summary_extract.py
cp lib/summary_render.py insider/summary_render.py
cp lib/templates/summary.html insider/templates/summary.html

zip -r insider.zip insider

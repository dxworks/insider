#!/bin/bash
set -e

VERSION=${1:-}

if [ -n "$VERSION" ]; then
  echo "$VERSION" > src/main/resources/insider-version
fi

gradle clean build

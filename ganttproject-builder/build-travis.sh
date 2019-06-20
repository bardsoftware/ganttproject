#!/bin/bash
find . -path */mvn/* -exec rm '{}' ';'
git pull --tags origin
./gradlew distbin
build-bin/build-package.sh build ganttproject-builder/dist-bin/ 2.99.0
ls -l build/dist
 

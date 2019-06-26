#!/bin/bash
find . -path */mvn/* -exec rm '{}' ';'
git pull --tags origin
git submodule update --init
./gradlew distbin
build-bin/package-lin.sh build ganttproject-builder/dist-bin/ 2.99.0
ls -l build/dist


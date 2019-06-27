#!/bin/bash
find . -path */mvn/* -exec rm '{}' ';'
git pull --tags origin
git submodule update --init
./gradlew distbin

./gradlew -b package.gradle build
ls -l build/dist


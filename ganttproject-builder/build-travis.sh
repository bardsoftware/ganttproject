#!/bin/bash
find . -path */mvn/* -exec rm '{}' ';'
git pull --tags origin
./gradlew build 

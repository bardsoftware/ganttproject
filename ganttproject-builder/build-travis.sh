#!/bin/bash
find . -path */mvn/* -exec rm '{}' ';'
git pull --tags origin
gradle build

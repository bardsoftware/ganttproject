#!/bin/bash
find . -path */mvn/* -exec rm '{}' ';'
cd ganttproject-builder
git pull --tags origin
gradle updateMavenDeps
gradle build

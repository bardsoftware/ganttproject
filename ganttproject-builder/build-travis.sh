#!/bin/bash
cd ganttproject-builder
git pull --tags origin
gradle updateLibs
gradle build

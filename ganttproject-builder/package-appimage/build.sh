#!/bin/bash

cat appimage-tpl.yaml | sed 's@{{PWD}}@'"$(pwd)"'@' > appimage.yaml
mkdir AppDir
appimage-builder --recipe appimage.yaml --skip-test

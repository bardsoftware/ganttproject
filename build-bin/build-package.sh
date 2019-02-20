#!/bin/bash

set -e

JLINK=${JAVA_HOME}/bin/jlink
MODULE_PATH=${JAVA_HOME}/jmods
PACKAGER=${1}
INSTALLER_TYPE=${2}
INPUT=${3}
OUTPUT=${4}
VERSION=${5}
FILE_ASSOCIATIONS=${6}
APP_ICON=${7}
EXTRA_BUNDLER_ARGUMENTS=${8}

rm -rf "${OUTPUT}/runtime"

${JLINK} \
  --add-modules java.base,java.datatransfer,java.desktop,java.logging,java.naming,java.security.jgss,java.xml,jdk.charsets,jdk.unsupported \
  --module-path ${MODULE_PATH} \
  --no-header-files --no-man-pages \
  --output "${OUTPUT}/runtime" \
  --strip-debug \
  --compress=2

${PACKAGER} \
  create-installer ${INSTALLER_TYPE} \
  --verbose \
  --echo-mode \
  --input "${INPUT}" \
  --output "${OUTPUT}/dist" \
  --name GanttProject \
  --main-jar eclipsito.jar \
  --class org.bardsoftware.eclipsito.Boot \
  --version ${VERSION} \
  --file-associations ${FILE_ASSOCIATIONS} \
  --icon $APP_ICON \
  --arguments "-plugins-dir plugins-${VERSION} -app net.sourceforge.ganttproject.GanttProject" \
  --identifier biz.ganttproject \
  --description "Free desktop project scheduling and project management application" \
  --category "Office" \
  --copyright "Copyright 2019 BarD Software s.r.o" \
  --vendor "BarD Software s.r.o" \
  --license-file LICENSE \
  --linux-deb-maintainer "Dmitry Barashev, BarD Software s.r.o" \
  --runtime-image "${OUTPUT}/runtime" \
  ${EXTRA_BUNDLER_ARGUMENTS}

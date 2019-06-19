#!/bin/bash

set -e

OUTPUT=${1}

JLINK=${JAVA_HOME}/bin/jlink
MODULE_PATH=${JAVA_HOME}/jmods

rm -rf "${OUTPUT}/runtime"

${JLINK} \
  --add-modules java.base,java.datatransfer,java.desktop,java.logging,java.naming,java.net.http,java.security.jgss,java.xml,jdk.charsets,jdk.unsupported \
  --module-path ${MODULE_PATH} \
  --no-header-files --no-man-pages \
  --output "${OUTPUT}/runtime" \
  --strip-debug \
  --compress=2

if [[ -z "${2}" ]]; then
  echo "Packaging was not requested. Bye!";
  exit 0;
fi

PACKAGER=${2}
INSTALLER_TYPE=${3}
INPUT=${4}
VERSION=${5}
FILE_ASSOCIATIONS=${6}
APP_ICON=${7}
EXTRA_BUNDLER_ARGUMENTS=${8}

${PACKAGER} \
  create-installer ${INSTALLER_TYPE} \
  --verbose \
  --echo-mode \
  --input "${INPUT}" \
  --output "${OUTPUT}/dist" \
  --name GanttProject \
  --main-jar eclipsito.jar \
  --class com.bardsoftware.eclipsito.Launch \
  --version ${VERSION} \
  --file-associations ${FILE_ASSOCIATIONS} \
  --icon $APP_ICON \
  --arguments "--verbosity 4 --version-dirs ./ --app net.sourceforge.ganttproject.GanttProject" \
  --identifier biz.ganttproject \
  --description "Free desktop project scheduling and project management application" \
  --category "Office" \
  --copyright "Copyright 2019 BarD Software s.r.o" \
  --vendor "BarD Software s.r.o" \
  --license-file LICENSE \
  --linux-deb-maintainer "Dmitry Barashev, BarD Software s.r.o" \
  --runtime-image "${OUTPUT}/runtime" \
  ${EXTRA_BUNDLER_ARGUMENTS}

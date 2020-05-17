#!/bin/bash

set -e

OUTPUT=${1:-/workspace}
INPUT=${2:-"./ganttproject-builder/dist-bin"}
VERSION=${3:-"2.8.11"}

echo "JAVA_HOME=$JAVA_HOME"
echo "Listing java binaries:"
ls "${JAVA_HOME}"/bin/

JLINK=${JAVA_HOME}/bin/jlink


test_runtime() {
  echo "Testing Java Runtime"
  ${OUTPUT}/runtime/bin/java -version
  ${OUTPUT}/runtime/bin/java --list-modules
}

test_runtime || {
  rm -rf ${OUTPUT}/runtime
  echo "Building Java Runtime"
  ${JLINK} \
    --add-modules java.base,java.datatransfer,java.desktop,java.logging,java.naming,java.net.http,java.security.jgss,java.xml,jdk.charsets,jdk.unsupported,jdk.unsupported.desktop,javafx.controls,javafx.swing,javafx.web \
    --no-header-files --no-man-pages \
    --output "${OUTPUT}/runtime" \
    --strip-debug \
    --compress=2
  test_runtime
}

# We remove JavaFX jars from the binary distro because they will be in the runtime
find "${INPUT}" -name 'javafx*.jar.lib' -delete

mkdir -p ${OUTPUT}/ganttproject
cp -R ${INPUT}/{eclipsito.jar,lib,logging.properties,plugins} ${OUTPUT}/ganttproject
mkdir -p ${OUTPUT}/resources
cp build-cfg/ganttproject.icns ${OUTPUT}/resources 
cp build-bin/mac/ganttproject-platypus-jre.command ${OUTPUT}/ganttproject.command

platypus -P build-bin/mac/GanttProject.platypus -y ${OUTPUT}/GanttProject.app
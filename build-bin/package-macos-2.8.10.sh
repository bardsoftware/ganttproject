#!/bin/bash

set -e

OUTPUT=${1}
INPUT=${2}
VERSION=${3}

JLINK=${JAVA_HOME}/bin/jlink
MODULE_PATH=${JAVA_HOME}/jmods

rm -rf "${OUTPUT}/runtime"

echo "Building Java Runtime"
${JLINK} \
  --add-modules java.base,java.datatransfer,java.desktop,java.logging,java.naming,java.net.http,java.security.jgss,java.xml,jdk.charsets,jdk.unsupported \
  --module-path ${MODULE_PATH} \
  --no-header-files --no-man-pages \
  --output "${OUTPUT}/runtime" \
  --strip-debug \
  --compress=2

echo "Testing Java Runtime"
${OUTPUT}/runtime/bin/java -version
cp ganttproject-builder/ganttproject build/runtime/bin/java

echo "Building packages"
java --module-path build-bin/mac/ \
  --add-opens jdk.jlink/jdk.tools.jlink.internal.packager=jdk.packager \
  -m jdk.packager/jdk.packager.Main \
  create-image  \
  --verbose \
  --echo-mode \
  --input "${INPUT}" \
  --output "${OUTPUT}/dist" \
  --name GanttProject \
  --main-jar eclipsito.jar \
  --class org.bardsoftware.eclipsito.Boot \
  --version ${VERSION} \
  --jvm-args "-Dapple.laf.useScreenMenuBar=true -Dcom.apple.macos.useScreenMenuBar=true	-Dcom.apple.mrj.application.apple.menu.about.name=GanttProject -Xdock:name=GanttProject -Xmx512m -ea -Dfile.encoding=UTF-8" \
  --arguments "-log true -plugins-dir plugins-2.8.10 -app net.sourceforge.ganttproject.GanttProject" \
  --identifier com.bardsoftware.ganttproject \
  --icon build-cfg/ganttproject.icns \
  --category "Office" \
  --copyright "Copyright 2019 BarD Software s.r.o" \
  --runtime-image "${OUTPUT}/runtime" \
  --mac-bundle-identifier com.bardsoftware.ganttproject 

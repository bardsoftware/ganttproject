#!/bin/bash

set -e

OUTPUT=${1}
INPUT=${2}
VERSION=${3}
JAVAFX_MODS_PATH=${4}

echo "JAVA_HOME=$JAVA_HOME"
echo "Listing java binaries:"
ls "${JAVA_HOME}"/bin/

JLINK=${JAVA_HOME}/bin/jlink

echo "Listing java mods:"
ls "${JAVA_HOME}"/jmods/
MODULE_PATH=${JAVA_HOME}/jmods:${JAVAFX_MODS_PATH}

rm -rf "${OUTPUT}/runtime"

echo "Building Java Runtime"
${JLINK} \
  --add-modules java.base,java.datatransfer,java.desktop,java.logging,java.naming,java.net.http,java.security.jgss,java.xml,jdk.charsets,jdk.unsupported,jdk.unsupported.desktop,javafx.controls,javafx.swing,javafx.web \
  --module-path ${MODULE_PATH} \
  --no-header-files --no-man-pages \
  --output "${OUTPUT}/runtime" \
  --strip-debug \
  --compress=2

echo "Testing Java Runtime"
${OUTPUT}/runtime/bin/java -version
${OUTPUT}/runtime/bin/java --list-modules

# We remove JavaFX jars from the binary distro because they will be in the runtime
find "${INPUT}" -name 'javafx*.jar.lib' -delete

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
  --class com.bardsoftware.eclipsito.Launch \
  --version ${VERSION} \
  --jvm-args "-Dapple.laf.useScreenMenuBar=true -Dcom.apple.macos.useScreenMenuBar=true	-Dcom.apple.mrj.application.apple.menu.about.name=GanttProject -Xdock:name=GanttProject -Xmx512m -ea -Dfile.encoding=UTF-8" \
  --arguments "--verbosity 4 --version-dirs plugins --app net.sourceforge.ganttproject.GanttProject" \
  --identifier com.bardsoftware.ganttproject \
  --icon build-cfg/ganttproject.icns \
  --category "Office" \
  --copyright "Copyright 2020 BarD Software s.r.o" \
  --runtime-image "${OUTPUT}/runtime" \
  --mac-bundle-identifier com.bardsoftware.ganttproject


cd "${OUTPUT}/dist"
zip -r ganttproject-${VERSION}.zip GanttProject.app
rm -r GanttProject.app

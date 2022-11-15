#!/bin/bash

set -e

OUTPUT=${1:-/workspace}
INPUT=${2:-"./ganttproject-builder/dist-bin"}
VERSION=${3:-"3.3.3290"}
RUNTIME_ARG=${4:-"--runtime"}



test_runtime() {
  echo "Testing Java Runtime"
  ${OUTPUT}/runtime/bin/java -version
  ${OUTPUT}/runtime/bin/java --list-modules
}

if [[ "$RUNTIME_ARG" == "--runtime" ]]; then
  echo "JAVA_HOME=$JAVA_HOME"
  echo "Listing java binaries:"
  ls "${JAVA_HOME}"/bin/

  JLINK=${JAVA_HOME}/bin/jlink
  test_runtime || {
    rm -rf ${OUTPUT}/runtime
    echo "Building Java Runtime"
    ${JLINK} \
      --add-modules java.base,java.datatransfer,java.desktop,java.logging,java.naming,java.net.http,java.security.jgss,java.sql,java.xml,jdk.charsets,jdk.crypto.ec,jdk.localedata,jdk.unsupported,jdk.unsupported.desktop,javafx.controls,javafx.swing,javafx.web \
      --no-header-files --no-man-pages \
      --output "${OUTPUT}/runtime" \
      --strip-debug \
      --compress=2
    test_runtime
    exit 0
  }
  # We remove JavaFX jars from the binary distro because they will be in the runtime
  # find "${INPUT}" -name 'javafx*.jar.lib' -delete
else
  echo "Skipping Java Runtime building"
fi

(cd .. && rm -rf ganttproject-$VERSION && unzip ganttproject-$VERSION.zip && rm -rf /tmp/plugins/ && rm -rf ganttproject/build/GanttProject.app/ )
mv ${INPUT}/plugins /tmp
jpackage --type app-image \
    --name GanttProject \
    --input ${INPUT} \
    --dest ${OUTPUT} \
    --java-options '-Dapple.laf.useScreenMenuBar=true 
                    -Dcom.apple.macos.useScreenMenuBar=true 
                    -Dcom.apple.mrj.application.apple.menu.about.name=GanttProject 
                    -Xdock:name=GanttProject 
                    -Xdock:icon=$APPDIR/ganttproject.icns 
                    -Xmx2048m
                    -Dfile.encoding=UTF-8
                    --add-exports javafx.controls/com.sun.javafx.scene.control.behavior=ALL-UNNAMED
                    --add-exports javafx.base/com.sun.javafx=ALL-UNNAMED
                    --add-exports javafx.base/com.sun.javafx.logging=ALL-UNNAMED
                    --add-exports javafx.controls/com.sun.javafx.scene.control=ALL-UNNAMED
                    --add-exports javafx.controls/com.sun.javafx.scene.control.skin=ALL-UNNAMED
                    --add-exports javafx.controls/com.sun.javafx.scene.control.skin.resources=ALL-UNNAMED
                    --add-exports javafx.controls/com.sun.javafx.scene.control.inputmap=ALL-UNNAMED
                    --add-exports javafx.graphics/com.sun.javafx.scene.traversal=ALL-UNNAMED
                    --add-exports javafx.graphics/com.sun.javafx.scene=ALL-UNNAMED
                    --add-exports javafx.graphics/com.sun.javafx.tk=ALL-UNNAMED
                    --add-exports javafx.graphics/com.sun.javafx.util=ALL-UNNAMED
                    --add-opens java.desktop/sun.swing=ALL-UNNAMED
                    -classpath $APPDIR:$APPDIR/eclipsito.jar:$APPDIR/lib/slf4j-api-2.0.3.jar:$APPDIR/lib/slf4j-jdk14-2.0.3.jar
                    -Duser.dir=$APPDIR
                    -DversionDirs=plugins:~/.ganttproject.d/updates
                    -Dapp=net.sourceforge.ganttproject.GanttProject' \
    --arguments '--verbosity 4' \
    --arguments '--version-dirs plugins:~/.ganttproject.d/updates' \
    --arguments '--app net.sourceforge.ganttproject.GanttProject' \
    --copyright 'Copyright (C) 2022 BarD Software s.r.o.' \
    --app-version ${VERSION} \
    --runtime-image ${OUTPUT}/runtime \
    --icon build-cfg/ganttproject.icns \
    --file-associations build-cfg/FAganttproject.properties \
    --mac-package-identifier com.bardsoftware.ganttproject \
    --main-class com.bardsoftware.eclipsito.Launch  --main-jar eclipsito.jar

mv /tmp/plugins ${OUTPUT}/GanttProject.app/Contents/app
cp build-cfg/ganttproject.icns ${OUTPUT}/GanttProject.app/Contents/app


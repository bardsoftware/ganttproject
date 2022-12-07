#!/bin/bash

set -e

OUTPUT=${1:-"build"}
INPUT=${2:-"./ganttproject-builder/dist-bin"}
VERSION=${3:-"$(cat ${INPUT}/plugins/base/VERSION)"}
APP_NAME=${MAC_APP_NAME:-"GanttProject"}
echo "Building GanttProject $VERSION macOS package from $INPUT"
mkdir -p "$OUTPUT" tmp

test_runtime() {
  echo "Testing Java Runtime"
  "${OUTPUT}"/runtime/bin/java -version
  "${OUTPUT}"/runtime/bin/java --list-modules
}

build_runtime() {
    rm -rf "${OUTPUT}"/runtime
    echo "Building Java Runtime"
    jlink \
      --add-modules java.base,java.datatransfer,java.desktop,java.logging,java.naming,java.net.http,java.security.jgss,java.sql,java.xml,jdk.charsets,jdk.crypto.ec,jdk.localedata,jdk.unsupported,jdk.unsupported.desktop,javafx.controls,javafx.swing,javafx.web \
      --no-header-files --no-man-pages \
      --output "${OUTPUT}/runtime" \
      --strip-debug \
      --compress=2
}

test_runtime || (build_runtime && test_runtime)

#(cd .. && rm -rf ganttproject-$VERSION && unzip ganttproject-$VERSION.zip && rm -rf /tmp/plugins/ && rm -rf ganttproject/build/GanttProject.app/ )
mv "${INPUT}"/plugins tmp
jpackage --type app-image \
    --name "${APP_NAME}" \
    --input "${INPUT}" \
    --dest "${OUTPUT}" \
    --java-options "-Dapple.laf.useScreenMenuBar=true
                    -Dcom.apple.macos.useScreenMenuBar=true 
                    -Dcom.apple.mrj.application.apple.menu.about.name=GanttProject
                    -Dsun.java2d.metal=true
                    -Xdock:name=$APP_NAME"' -Xdock:icon=$APPDIR/ganttproject.icns'" \
                    -Xmx2048m \
                    -Dfile.encoding=UTF-8
                    --add-exports javafx.controls/com.sun.javafx.scene.control.behavior=ALL-UNNAMED
                    --add-exports javafx.base/com.sun.javafx=ALL-UNNAMED
                    --add-exports javafx.base/com.sun.javafx.event=ALL-UNNAMED
                    --add-exports javafx.base/com.sun.javafx.logging=ALL-UNNAMED
                    --add-exports javafx.controls/com.sun.javafx.scene.control=ALL-UNNAMED
                    --add-exports javafx.controls/com.sun.javafx.scene.control.skin=ALL-UNNAMED
                    --add-exports javafx.controls/com.sun.javafx.scene.control.skin.resources=ALL-UNNAMED
                    --add-exports javafx.controls/com.sun.javafx.scene.control.inputmap=ALL-UNNAMED
                    --add-exports javafx.graphics/com.sun.javafx.scene.traversal=ALL-UNNAMED
                    --add-exports javafx.graphics/com.sun.javafx.scene=ALL-UNNAMED
                    --add-exports javafx.graphics/com.sun.javafx.tk=ALL-UNNAMED
                    --add-exports javafx.graphics/com.sun.javafx.util=ALL-UNNAMED
                    --add-opens java.desktop/sun.swing=ALL-UNNAMED \
                    -classpath "'$APPDIR:$APPDIR/eclipsito.jar:$APPDIR/lib/slf4j-api-2.0.4.jar:$APPDIR/lib/slf4j-jdk14-2.0.4.jar'"\
                    -Duser.dir="'$APPDIR'" -DversionDirs=plugins:~/.ganttproject.d/updates \
                    -Dapp=net.sourceforge.ganttproject.GanttProject
                    -Dorg.jooq.no-logo=true
                    -Dgpcloud=prod" \
    --arguments '--verbosity 4' \
    --arguments '--version-dirs plugins:~/.ganttproject.d/updates' \
    --arguments '--app net.sourceforge.ganttproject.GanttProject' \
    --copyright 'Copyright (C) 2022 BarD Software s.r.o.' \
    --app-version "${VERSION}" \
    --runtime-image "${OUTPUT}"/runtime \
    --icon build-cfg/ganttproject.icns \
    --file-associations build-cfg/FAganttproject.properties \
    --mac-package-identifier com.bardsoftware.ganttproject \
    --main-class com.bardsoftware.eclipsito.Launch  --main-jar eclipsito.jar

mv tmp/plugins "${OUTPUT}/${APP_NAME}.app/Contents/app"
cp build-cfg/ganttproject.icns "${OUTPUT}/${APP_NAME}.app/Contents/app"
echo "You can find the package in $OUTPUT"
ls $OUTPUT


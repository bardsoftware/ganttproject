#!/bin/bash

set -e

OUTPUT=${1:-"build"}
INPUT=${2:-"./ganttproject-builder/dist-bin"}
VERSION=${3:-"$(cat ${INPUT}/plugins/base/VERSION)"}
APP_NAME=${LIN_APP_NAME:-"GanttProject"}
echo "Building GanttProject $VERSION Linux App Image package from $INPUT"
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
      --strip-debug
}

test_runtime || (build_runtime && test_runtime)

#(cd .. && rm -rf ganttproject-$VERSION && unzip ganttproject-$VERSION.zip && rm -rf /tmp/plugins/ && rm -rf ganttproject/build/GanttProject.app/ )
mv "${INPUT}"/plugins tmp
jpackage --type app-image \
    --name "GanttProject" \
    --input "${INPUT}" \
    --dest "${OUTPUT}" \
    --java-options '-Dsun.java2d.metal=true
                    -Xmx2048m
                    -Dfile.encoding=UTF-8
                    --add-exports javafx.controls/com.sun.javafx.scene.control.behavior=ALL-UNNAMED
                    --add-exports javafx.base/com.sun.javafx=ALL-UNNAMED
                    --add-exports javafx.base/com.sun.javafx.event=ALL-UNNAMED
                    --add-exports javafx.base/com.sun.javafx.logging=ALL-UNNAMED
                    --add-exports javafx.controls/com.sun.javafx.scene.control=ALL-UNNAMED
                    --add-exports javafx.controls/com.sun.javafx.scene.control.skin=ALL-UNNAMED
                    --add-exports javafx.controls/com.sun.javafx.scene.control.skin.resources=ALL-UNNAMED
                    --add-exports javafx.controls/com.sun.javafx.scene.control.inputmap=ALL-UNNAMED
                    --add-exports javafx.graphics/com.sun.javafx.application=ALL-UNNAMED
                    --add-exports javafx.graphics/com.sun.glass.ui=ALL-UNNAMED
                    --add-exports javafx.graphics/com.sun.javafx.scene.traversal=ALL-UNNAMED
                    --add-exports javafx.graphics/com.sun.javafx.scene=ALL-UNNAMED
                    --add-exports javafx.graphics/com.sun.javafx.tk=ALL-UNNAMED
                    --add-exports javafx.graphics/com.sun.javafx.util=ALL-UNNAMED
                    --add-opens java.desktop/sun.swing=ALL-UNNAMED
                    -classpath $APPDIR:$APPDIR/eclipsito.jar:$APPDIR/lib/slf4j-api-2.0.17.jar:$APPDIR/lib/logback-classic-1.5.18.jar:$APPDIR/lib/logback-core-1.5.18.jar
                    -Duser.dir=$APPDIR -DversionDirs=plugins:~/.ganttproject.d/updates
                    -Dapp=net.sourceforge.ganttproject.GanttProject
                    -Dorg.jooq.no-logo=true
                    -Dgpcloud=prod' \
    --arguments '--verbosity 4' \
    --arguments '--version-dirs plugins:~/.ganttproject.d/updates' \
    --arguments '--app net.sourceforge.ganttproject.GanttProject' \
    --copyright 'Copyright (C) 2026 BarD Software s.r.o.' \
    --vendor 'BarD Software s.r.o.' \
    --app-version "${VERSION}" \
    --runtime-image "${OUTPUT}"/runtime \
    --icon build-cfg/ganttproject.png \
    --main-class com.bardsoftware.eclipsito.Launch  --main-jar eclipsito.jar

cp build-cfg/GanttProject.desktop $OUTPUT/GanttProject
cp build-cfg/ganttproject.svg $OUTPUT/GanttProject/

mv tmp/plugins $OUTPUT/GanttProject/lib/app/
mv appimagetool $OUTPUT
rm $OUTPUT/GanttProject/lib/app/lib/javafx-*
ls $OUTPUT

cd $OUTPUT/GanttProject
mv bin app
ln -s app/GanttProject AppRun
cd ..
./appimagetool GanttProject/ "ganttproject-${VERSION}.AppImage"
rm -rf GanttProject/ runtime/ appimagetool
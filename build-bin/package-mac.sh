#!/bin/bash

set -e

OUTPUT=${1:-/workspace}
INPUT=${2:-"./ganttproject-builder/dist-bin"}
VERSION=${3:-"3.0"}
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
      --add-modules java.base,java.datatransfer,java.desktop,java.logging,java.naming,java.net.http,java.security.jgss,java.xml,jdk.charsets,jdk.crypto.ec,jdk.localedata,jdk.unsupported,jdk.unsupported.desktop,javafx.controls,javafx.swing,javafx.web \
      --no-header-files --no-man-pages \
      --output "${OUTPUT}/runtime" \
      --strip-debug \
      --compress=2
    test_runtime
  }
  # We remove JavaFX jars from the binary distro because they will be in the runtime
  # find "${INPUT}" -name 'javafx*.jar.lib' -delete
else
  echo "Skipping Java Runtime building"
fi

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
                    -Xmx1024m 
                    -Dfile.encoding=UTF-8
                    -Duser.dir=$APPDIR' \
    --arguments '--verbosity 4' \
    --arguments '--version-dirs plugins:~/.ganttproject.d/updates' \
    --arguments '--app net.sourceforge.ganttproject.GanttProject' \
    --copyright 'Copyright (C) 2020 BarD Software s.r.o.' \
    --app-version ${VERSION} \
    --runtime-image ${OUTPUT}/runtime \
    --icon build-cfg/ganttproject.icns \
    --file-associations build-cfg/FAganttproject.properties \
    --mac-package-identifier com.bardsoftware.ganttproject \
    --main-class com.bardsoftware.eclipsito.Launch  --main-jar eclipsito.jar

mv /tmp/plugins ${OUTPUT}/GanttProject.app/Contents/app
cp build-cfg/ganttproject.icns ${OUTPUT}/GanttProject.app/Contents/app

# jpackage --type dmg \
#     --app-image build/GanttProject.app \
#     --license-file ganttproject-builder/LICENSE \
#     -n "GanttProject 2.8.11" \
#      --mac-sign \
#      --mac-signing-key-user-name "Developer ID Application: BarD Software s.r.o"

# find ${OUTPUT}/GanttProject.app -type f \
#   -not -path "*/Contents/runtime/*" \
#   -not -path "*/Contents/MacOS/GanttProject" \
#   -not -path "*libapplauncher.dylib" \
#   -exec codesign --timestamp --entitlements build-cfg/ganttproject.entitlements.xml -s "Developer ID Application: BarD Software s.r.o" --prefix com.bardsoftware.ganttproject --options runtime -v \;

# mkdir -p ${OUTPUT}/ganttproject
# cp -R ${INPUT}/{eclipsito.jar,lib,logging.properties,plugins} ${OUTPUT}/ganttproject
# mkdir -p ${OUTPUT}/resources
# cp build-cfg/ganttproject.icns ${OUTPUT}/resources 
# if [[ "$RUNTIME_ARG" == "--runtime" ]]; then
#   cp build-bin/mac/ganttproject-platypus-jre.command ${OUTPUT}/ganttproject.command
# elif [[ "$RUNTIME_ARG" == "--no-runtime" ]]; then
#   cp ganttproject-builder/ganttproject.command ${OUTPUT}/ganttproject.command
# fi

# platypus -P build-bin/mac/GanttProject.platypus -y ${OUTPUT}/GanttProject.app
# cp build-cfg/Info.plist ${OUTPUT}/GanttProject.app/Contents/

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

# Now we build DEB and RPM packages
echo "Building package for Debian"
mkdir $OUTPUT/dist
cd $GITHUB_WORKSPACE/build-bin
ant -f build-deb.xml dist-deb-full

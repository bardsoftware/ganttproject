#!/bin/bash
set -o errexit

REPO_ROOT=$(pwd)
echo "Work dir: $REPO_ROOT"
mkdir -p build/distributions
ls -l $REPO_ROOT/build/distributions
VERSION=$(cat ganttproject-builder/BUILD-HISTORY-MINOR | tail -n 1 | awk '{print $2}')
[[ -z "$VERSION" ]] && exit 1
echo "Building version $VERSION"
MODULES=$(cat ganttproject-builder/BUILD-HISTORY-MINOR | tail -n 1 | awk '{print $3}')
[[ -z "$MODULES" ]] && exit 1
echo "Will pick these modules: $MODULES"
echo "3.3.$VERSION" > ganttproject-builder/VERSION
./gradlew clean distbin
cd ganttproject-builder/dist-bin/plugins/
mkdir update-$VERSION
IFS=','
for m in $MODULES; do cp -R base/$m update-$VERSION/ ; done
echo "3.3.$VERSION" > update-$VERSION/VERSION
zip -r "update-$VERSION.zip" "update-$VERSION"
gsutil cp "update-$VERSION.zip" gs://dl.ganttproject.biz/updates/ && gsutil acl ch -u AllUsers:R "gs://dl.ganttproject.biz/updates/update-$VERSION.zip";

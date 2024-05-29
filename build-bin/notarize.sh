#!/bin/bash

VER=$1
SIG=$2
NOTARIZE_PASSWORD=$3
COMMAND=$4
KEYCHAIN=${5:-"~/Library/Keychains/login.keychain-db"}

do_prepare() {
find build/GanttProject.app/ -type f -not -path *Contents/runtime/* -not -path */Contents/MacOS/GanttProject -not -path *libapplauncher.dylib -exec codesign --timestamp -f -s "$SIG" --prefix com.bardsoftware. --entitlements build-cfg/ganttproject.entitlements.xml --options runtime -v --keychain "$KEYCHAIN" {} \;
find build/GanttProject.app/Contents/runtime -type f -not -path */legal/* -not -path */man/* -exec codesign --timestamp -f -s "$SIG" --prefix com.bardsoftware. --entitlements build-cfg/ganttproject.entitlements.xml --options runtime -v --keychain "$KEYCHAIN" {} \;
codesign -f --timestamp --entitlements build-cfg/ganttproject.entitlements.xml -s "$SIG" --prefix com.bardsoftware. --options runtime -v build/GanttProject.app/Contents/runtime
codesign -f --timestamp --entitlements build-cfg/ganttproject.entitlements.xml -s "$SIG" --prefix com.bardsoftware. --options runtime -v build/GanttProject.app
codesign -vvv --deep --strict build/GanttProject.app
spctl -a -t exec -vv build/GanttProject.app
}

do_notarize() {
  xcrun notarytool submit --apple-id contact@bardsoftware.com --team-id QDCH4KTVP7  --password $NOTARIZE_PASSWORD --wait build/ganttproject-1.0.dmg
}

do_staple() {
	xcrun stapler staple build/GanttProject.app
}

case $COMMAND in
sign)
    do_prepare
    ;;
notarize)
    do_notarize
    ;;
staple)
    do_staple
    ;;
*)
    echo "Unknown command: $COMMAND" && exit 1
    ;;
esac

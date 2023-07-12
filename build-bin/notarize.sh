#!/bin/bash

VER=$1
SIG=$2
NOTARIZE_PASSWORD=$3
COMMAND=$4

do_prepare() {
#find build/GanttProject.app/ -type f -not -path *Contents/runtime/* -not -path */Contents/MacOS/GanttProject -not -path *libapplauncher.dylib -exec codesign --timestamp -f -s "$SIG" --prefix com.bardsoftware. --entitlements build-cfg/ganttproject.entitlements.xml --options runtime -v --keychain ~/Library/Keychains/login.keychain-db {} \;
#find build/GanttProject.app/Contents/runtime -type f -not -path */legal/* -not -path */man/* -exec codesign --timestamp -f -s "$SIG" --prefix com.bardsoftware. --entitlements build-cfg/ganttproject.entitlements.xml --options runtime -v --keychain ~/Library/Keychains/login.keychain-db {} \;
#codesign -f --timestamp --entitlements build-cfg/ganttproject.entitlements.xml -s "$SIG" --prefix com.bardsoftware. --options runtime -v build/GanttProject.app/Contents/runtime
#codesign -f --timestamp --entitlements build-cfg/ganttproject.entitlements.xml -s "$SIG" --prefix com.bardsoftware. --options runtime -v build/GanttProject.app
#codesign -vvv --deep --strict build/GanttProject.app
#spctl -a -t exec -vv build/GanttProject.app

security dump-keychain build.keychain
}

do_notarize() {
xcrun altool --notarize-app --primary-bundle-id com.bardsoftware.ganttproject --file ./ganttproject-$VER.dmg --username contact@bardsoftware.com --password $NOTARIZE_PASSWORD
xcrun altool --notarization-history 0 -u contact@bardsoftware.com -p $NOTARIZE_PASSWORD
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

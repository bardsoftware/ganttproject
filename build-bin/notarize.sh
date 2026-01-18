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

do_all2() {
    echo "------------------ PREPARING KEYCHAINS ------------------------"
    echo $MACOS_CERTIFICATE | base64 --decode > certificate.p12

    security create-keychain -p "$MACOS_CI_KEYCHAIN_PWD" build.keychain
    security default-keychain -s build.keychain
    security unlock-keychain -p "$MACOS_CI_KEYCHAIN_PWD" build.keychain
    security import certificate.p12 -k build.keychain -P "$MACOS_CERTIFICATE_PWD" -T /usr/bin/codesign
    security set-key-partition-list -S apple-tool:,apple:,codesign: -s -k "$MACOS_CI_KEYCHAIN_PWD" build.keychain

    echo "------------------ SIGNING ------------------------"
    do_prepare
    jpackage --type dmg --app-image build/GanttProject.app -n "ganttproject" --dest build/

    echo "------------------ NOTARIZING ------------------------"
    do_notarize
    do_staple
    rm build/*.dmg
}

do_all() {
    echo "------------------ PREPARING KEYCHAINS ------------------------"
    # Create a temporary keychain
    KEYCHAIN="build.keychain"
    CERT_P12="certificate.p12"

    echo "$MACOS_CERTIFICATE" | base64 --decode > "$CERT_P12"

    security create-keychain -p "$MACOS_CI_KEYCHAIN_PWD" "$KEYCHAIN"
    security default-keychain -s "$KEYCHAIN"
    security unlock-keychain -p "$MACOS_CI_KEYCHAIN_PWD" "$KEYCHAIN"
    security import "$CERT_P12" -k "$KEYCHAIN" -P "$MACOS_CERTIFICATE_PWD" -T /usr/bin/codesign
    security set-key-partition-list -S apple-tool:,apple:,codesign: -s -k "$MACOS_CI_KEYCHAIN_PWD" "$KEYCHAIN"
    security set-keychain-settings -t 3600 -u "$KEYCHAIN"

    echo "------------------ LISTING KEYS ------------------------"
    security find-identity -v -p codesigning "$KEYCHAIN"

    echo "------------------ SIGNING ------------------------"
    # Update KEYCHAIN variable so do_prepare uses our new keychain
    # In notarize.sh, KEYCHAIN is defined at the top as:
    # KEYCHAIN=${5:-"~/Library/Keychains/login.keychain-db"}
    # We want do_prepare to use the local build.keychain we just created.
    # We can either export it or pass it if do_prepare was designed for it.
    # do_prepare uses $KEYCHAIN in its codesign calls.
    
    do_prepare
    jpackage --type dmg --app-image build/GanttProject.app -n "ganttproject" --dest build/

    echo "------------------ NOTARIZING ------------------------"
    do_notarize
    do_staple
    rm build/*.dmg
    rm "$CERT_P12"
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
all)
    do_all
    ;;
all2)
    do_all2
    ;;
*)
    echo "Unknown command: $COMMAND" && exit 1
    ;;
esac

#!/bin/bash

VER=$1
SIG=$2
NOTARIZE_PASSWORD=$3
COMMAND=$4
KEYCHAIN=${5:-"~/Library/Keychains/login.keychain-db"}

do_sign() {
  APP_PATH="build/GanttProject.app"
  RUNTIME_PATH="$APP_PATH/Contents/runtime"
  ENTITLEMENTS="build-cfg/ganttproject.entitlements.xml"

  echo "Signing Java runtime..."
  find "$RUNTIME_PATH" -type f \( -name "*.dylib" -or -name "*.so" -or -perm +111 \)
  echo "... go! ..."
  #find "$APP_PATH" -type f \( -name "*.dylib" -o -perm +0111 \) -exec codesign --force --options runtime --entitlements "$ENTITLEMENTS" --sign "$SIG" --timestamp {} \;
  find "$RUNTIME_PATH" -type f \( -name "*.dylib" -or -name "*.so" -or -perm +111 \) -exec codesign --timestamp -f -s "$SIG" --prefix com.bardsoftware. --entitlements "$ENTITLEMENTS" --options runtime -v --keychain "$KEYCHAIN" {} \;
  #codesign -f --timestamp --entitlements "$ENTITLEMENTS" -s "$SIG" --prefix com.bardsoftware. --options runtime -v --keychain "$KEYCHAIN" "$RUNTIME_PATH"
  echo "...done"
  echo "----------------------------------"

  echo "Signing libraries and executables..."
  find "$APP_PATH" -type f \( -name "*.dylib" -or -name "*.so" -or -perm +111 \) -not -path "$RUNTIME_PATH/*"
  echo "... go! ..."
  find "$APP_PATH" -type f \( -name "*.dylib" -or -name "*.so" -or -perm +111 \) -not -path "$RUNTIME_PATH/*" -exec codesign --timestamp -f -s "$SIG" --prefix com.bardsoftware. --entitlements "$ENTITLEMENTS" --options runtime -v --keychain "$KEYCHAIN" {} \;
  echo "...done"
  echo "----------------------------------"


  echo "Signing the application bundle..."
  codesign -f --timestamp --entitlements "$ENTITLEMENTS" -s "$SIG" --prefix com.bardsoftware. --options runtime -v --keychain "$KEYCHAIN" "$APP_PATH"

  echo "Verifying signature..."
  codesign -f -vvv --deep --strict "$APP_PATH"
  spctl -a -t exec -vv "$APP_PATH"
}

do_notarize() {
  xcrun notarytool submit --apple-id contact@bardsoftware.com --team-id QDCH4KTVP7  --password $NOTARIZE_PASSWORD --wait build/out.zip
}

do_staple() {
	xcrun stapler staple build/GanttProject.app
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
    do_sign
    cd build
    zip -r out.zip GanttProject.app
    cd ..

    echo "------------------ NOTARIZING ------------------------"
    do_notarize
    do_staple
    rm build/*.zip
    rm "$CERT_P12"
}

case $COMMAND in
sign)
    do_sign
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
*)
    echo "Unknown command: $COMMAND" && exit 1
    ;;
esac

#!/bin/sh
set -eu
cd "$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)"
if [ "$(uname -s)" != Darwin ]; then
    printf 'This script packages the macOS app. Use the JAR on other platforms.\n' >&2
    exit 1
fi
./build.sh build
if [ -n "${JAVA_HOME:-}" ]; then
    packager="$JAVA_HOME/bin/jpackage"
else
    packager=jpackage
fi
# Only replaces the generated app in the repository's build directory.
rm -rf build/app/KingdomKing.app
"$packager" --type app-image --name KingdomKing --app-version 1.0.0 \
    --input dist --main-jar KingdomKing.jar --main-class game.rain.Game \
    --dest build/app --add-modules java.desktop \
    --mac-package-identifier com.samanimkr.kingdomking \
    --mac-package-name KingdomKing --mac-app-category games \
    --description "KingdomKing - the original Java pixel-art exploration game" \
    --vendor Samanimkr --copyright "Copyright (c) 2017 Samani Mukhtar"
printf '\nReady: build/app/KingdomKing.app (includes Java for this Mac architecture).\n'

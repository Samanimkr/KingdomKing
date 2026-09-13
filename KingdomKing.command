#!/bin/sh
cd "$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)" || exit 1
./run.sh "$@"
result=$?
if [ "$result" -ne 0 ]; then
    printf '\nKingdomKing could not start. Install a JDK 17 or newer, then try again.\nPress Return to close.\n'
    read -r answer
fi
exit "$result"

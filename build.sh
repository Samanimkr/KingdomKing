#!/bin/sh
set -eu
cd "$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)"
if [ -n "${JAVA_HOME:-}" ]; then
    exec "$JAVA_HOME/bin/java" tools/Build.java "$@"
fi
exec java tools/Build.java "$@"

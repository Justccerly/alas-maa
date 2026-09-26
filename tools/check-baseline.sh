#!/usr/bin/env bash

ROOT_DIR="$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)"
BASELINE="$ROOT_DIR/baseline.json"
MAA_APP="$ROOT_DIR/upstream/MaaFwApp"
EXPECTED_APP_COMMIT="5f5871095fde7a0af5a0171145f3c84d3e2eac18"

failures=0

check() {
    if "$@"; then
        printf '[ok] '
        printf '%s\n' "$*"
    else
        printf '[!!] '
        printf '%s\n' "$*"
        failures=$((failures + 1))
    fi
}

check test -f "$BASELINE"
check test -d "$MAA_APP"
check test "$(git -C "$MAA_APP" rev-parse HEAD 2>/dev/null)" = "$EXPECTED_APP_COMMIT"
check test -f "$MAA_APP/gradlew"
check test -f "$MAA_APP/scripts/setup_maa_framework.py"
check test -d "$ROOT_DIR/resources/pipeline"
check test -d "$ROOT_DIR/resources/templates"
check test -d "$ROOT_DIR/resources/maps"
check test -d "$ROOT_DIR/resources/game-data"

if test -d "$MAA_APP/app/src/main/jniLibs/arm64-v8a"; then
    check test -f "$MAA_APP/app/src/main/jniLibs/arm64-v8a/libMaaFramework.so"
else
    printf '[info] arm64 native libraries are not deployed yet\n'
fi

if test "$failures" -ne 0; then
    printf '%s\n' "Baseline check failed: $failures check(s) failed."
    exit 1
fi

printf '%s\n' 'Baseline structure is ready.'

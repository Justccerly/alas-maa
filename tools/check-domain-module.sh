#!/usr/bin/env bash
ROOT_DIR="$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)"
DOMAIN_DIR="$ROOT_DIR/alas-domain"
ADAPTER_DIR="$ROOT_DIR/alas-maafw"
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

check test -f "$DOMAIN_DIR/build.gradle.kts"
check grep -Fq 'id("maafw.kotlin.jvm")' "$DOMAIN_DIR/build.gradle.kts"
check test -d "$DOMAIN_DIR/src/main/kotlin"
check test -d "$DOMAIN_DIR/src/test/kotlin"
check grep -R -q 'class MapPathfinderTest' "$DOMAIN_DIR/src/test/kotlin"
check test -f "$DOMAIN_DIR/src/main/kotlin/com/justccerly/alas/domain/MapRecognition.kt"
check grep -R -q 'class MapSnapshotMapperTest' "$DOMAIN_DIR/src/test/kotlin"

check test -f "$ADAPTER_DIR/build.gradle.kts"
check test -d "$ADAPTER_DIR/src/main/kotlin"
check test -d "$ADAPTER_DIR/src/test/kotlin"
check grep -R -q 'class MapAdapterTest' "$ADAPTER_DIR/src/test/kotlin"
check grep -R -q 'MapActionPlanner' "$ADAPTER_DIR/src/main/kotlin"

if test "$failures" -ne 0; then
    printf 'Domain module check failed: %s check(s) failed.\n' "$failures"
    exit 1
fi
printf '%s\n' 'Alas Domain module structure is ready.'

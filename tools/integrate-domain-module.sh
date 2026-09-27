#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)"
HOST_DIR="${1:-$ROOT_DIR/upstream/MaaFwApp}"
DOMAIN_DIR="$ROOT_DIR/alas-domain"
ADAPTER_DIR="$ROOT_DIR/alas-maafw"

if [[ ! -d "$HOST_DIR" ]]; then
    printf 'Missing MaaFwApp host: %s\n' "$HOST_DIR" >&2
    exit 1
fi
if [[ ! -f "$DOMAIN_DIR/build.gradle.kts" ]]; then
    printf 'Missing Alas Domain Gradle module: %s\n' "$DOMAIN_DIR" >&2
    exit 1
fi
if [[ ! -f "$ADAPTER_DIR/build.gradle.kts" ]]; then
    printf 'Missing MaaFramework adapter module: %s\n' "$ADAPTER_DIR" >&2
    exit 1
fi

rm -rf "$HOST_DIR/alas-domain"
cp -a "$DOMAIN_DIR" "$HOST_DIR/alas-domain"
rm -rf "$HOST_DIR/alas-maafw"
cp -a "$ADAPTER_DIR" "$HOST_DIR/alas-maafw"

if ! grep -Fq 'include(":alas-domain")' "$HOST_DIR/settings.gradle.kts"; then
    printf '\ninclude(":alas-domain")\n' >> "$HOST_DIR/settings.gradle.kts"
fi
if ! grep -Fq 'include(":alas-maafw")' "$HOST_DIR/settings.gradle.kts"; then
    printf 'include(":alas-maafw")\n' >> "$HOST_DIR/settings.gradle.kts"
fi

if ! grep -Fq 'implementation(project(":alas-domain"))' "$HOST_DIR/app/build.gradle.kts"; then
    sed -i '/^dependencies[[:space:]]*{/a\    implementation(project(":alas-domain"))' "$HOST_DIR/app/build.gradle.kts"
fi
if ! grep -Fq 'implementation(project(":alas-maafw"))' "$HOST_DIR/app/build.gradle.kts"; then
    sed -i '/^dependencies[[:space:]]*{/a\    implementation(project(":alas-maafw"))' "$HOST_DIR/app/build.gradle.kts"
fi

printf '[ok] Alas Domain module integrated into %s\n' "$HOST_DIR"
printf '[ok] settings include: '
grep -F 'include(":alas-domain")' "$HOST_DIR/settings.gradle.kts"
printf '[ok] app dependency: '
grep -F 'implementation(project(":alas-domain"))' "$HOST_DIR/app/build.gradle.kts"
printf '[ok] adapter module: '
grep -F 'include(":alas-maafw")' "$HOST_DIR/settings.gradle.kts"
printf '[ok] adapter dependency: '
grep -F 'implementation(project(":alas-maafw"))' "$HOST_DIR/app/build.gradle.kts"

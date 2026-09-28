#!/usr/bin/env bash

ROOT_DIR="$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)"
MAA_APP="$ROOT_DIR/upstream/MaaFwApp"

if test ! -d "$MAA_APP"; then
    printf '%s\n' "Missing $MAA_APP. Clone MaaFwApp v0.1.0 first."
    exit 1
fi

cd "$MAA_APP"
export PI_PROFILE="$ROOT_DIR/pi-profile.yaml"
cd "$ROOT_DIR"
./tools/integrate-domain-module.sh "$MAA_APP"
./tools/integrate-maafw-recognition.sh "$MAA_APP"
./tools/check-maafw-recognition-integration.sh "$MAA_APP"
cd "$MAA_APP"
python3 scripts/setup_maa_framework.py --tag v5.10.5 --abi arm64-v8a
./gradlew :app:assembleDebug

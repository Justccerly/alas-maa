#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)"
HOST_DIR="${1:-$ROOT_DIR/upstream/MaaFwApp}"
LIB="$HOST_DIR/app/src/main/java/com/aliothmoon/maafw/maa/MaaFrameworkLibrary.kt"
RUNNER="$HOST_DIR/app/src/main/java/com/aliothmoon/maafw/remote/MaaRunner.kt"
CALLBACK="$HOST_DIR/app/src/main/java/com/aliothmoon/maafw/remote/AlasCustomRecognition.kt"
SINK="$HOST_DIR/app/src/main/java/com/aliothmoon/maafw/remote/AlasDecisionFileSink.kt"
SERVICE="$HOST_DIR/app/src/main/java/com/aliothmoon/maafw/remote/RemoteServiceImpl.kt"
PIPELINE="$ROOT_DIR/resources/resource/base/pipeline/map_action.json"

for path in "$LIB" "$RUNNER" "$CALLBACK" "$SINK" "$SERVICE" "$PIPELINE"; do
    test -f "$path" || { printf '[!!] missing %s\n' "$path" >&2; exit 1; }
done

grep -Fq 'fun MaaResourceRegisterCustomRecognition' "$LIB"
grep -Fq 'fun interface MaaCustomRecognitionCallback' "$LIB"
grep -Fq 'fun MaaStringBufferSet' "$LIB"
grep -Fq 'MaaResourceRegisterCustomRecognition(res, AlasCustomRecognition.NAME' "$RUNNER"
grep -Fq 'const val NAME = "AlasMapRecognition"' "$CALLBACK"
grep -Fq 'MaaMapRecognitionCallbackCodec.decodeRequest' "$CALLBACK"
# The decision sink must be reachable from the callback and installed before registration,
# otherwise a device run records nothing and the failure is silent.
grep -Fq 'AlasRecognitionDecisionLog.request' "$CALLBACK"
grep -Fq 'AlasRecognitionDecisionLog.result' "$CALLBACK"
grep -Fq 'AlasRecognitionDecisionLog.failure' "$CALLBACK"
grep -Fq 'AlasDecisionSinkHolder.sink' "$CALLBACK"
# Installed from setup(): that is the only place the privileged process learns a writable
# logDir. Reading AppPaths here would be an uninitialised lateinit in that process.
grep -Fq 'AlasDecisionFileSink.install(logDir)' "$SERVICE"

python3 - "$PIPELINE" <<'PY'
import json
import sys

with open(sys.argv[1], encoding="utf-8") as handle:
    document = json.load(handle)
node = document.get("AlasMapRecognition", {})
recognition = node.get("recognition", {})
param = recognition.get("param", {})
if recognition.get("type") != "Custom":
    raise SystemExit("AlasMapRecognition is not Custom")
if param.get("custom_recognition") != "AlasMapRecognition":
    raise SystemExit("custom recognition name mismatch")
request = param.get("custom_recognition_param", {})
map_data = request.get("map", {})
if not isinstance(map_data.get("cells"), list) or not map_data.get("cells"):
    raise SystemExit("custom recognition replay map is empty")
box = request.get("box", {})
if any(not isinstance(box.get(field), int) for field in ("x", "y", "width", "height")):
    raise SystemExit("custom recognition replay box is invalid")
if box["width"] <= 0 or box["height"] <= 0:
    raise SystemExit("custom recognition replay box dimensions must be positive")
print("[ok] Custom Recognition host integration symbols and replay entry")
PY

#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)"
HOST_DIR="${1:-$ROOT_DIR/upstream/MaaFwApp}"
LIB="$HOST_DIR/app/src/main/java/com/aliothmoon/maafw/maa/MaaFrameworkLibrary.kt"
RUNNER="$HOST_DIR/app/src/main/java/com/aliothmoon/maafw/remote/MaaRunner.kt"
TARGET="$HOST_DIR/app/src/main/java/com/aliothmoon/maafw/remote/AlasCustomRecognition.kt"

if [[ ! -f "$LIB" || ! -f "$RUNNER" ]]; then
    printf 'Missing MaaFwApp sources under %s\n' "$HOST_DIR" >&2
    exit 1
fi

mkdir -p "$(dirname -- "$TARGET")"
cp "$ROOT_DIR/host-integration/AlasCustomRecognition.kt" "$TARGET"

python3 - "$LIB" "$RUNNER" <<'PY'
from pathlib import Path
import sys

lib_path, runner_path = map(Path, sys.argv[1:])
lib = lib_path.read_text(encoding="utf-8")
runner = runner_path.read_text(encoding="utf-8")

if "MaaResourceRegisterCustomRecognition" not in lib:
    needle = "    fun MaaResourceAddSink(res: Pointer?, sink: MaaEventCallback?, transArg: Pointer?): Long\n"
    addition = needle + "\n    fun MaaResourceRegisterCustomRecognition(res: Pointer?, name: String, recognition: MaaCustomRecognitionCallback?, transArg: Pointer?): Byte\n"
    if needle not in lib:
        raise SystemExit("cannot find MaaResourceAddSink declaration")
    lib = lib.replace(needle, addition, 1)
if "fun MaaStringBufferSet(" not in lib:
    needle = "    fun MaaStringBufferGet(handle: Pointer?): String?\n"
    addition = needle + "\n    fun MaaStringBufferSet(handle: Pointer?, value: String): Byte\n"
    if needle not in lib:
        raise SystemExit("cannot find MaaStringBufferGet declaration")
    lib = lib.replace(needle, addition, 1)
if "fun interface MaaCustomRecognitionCallback" not in lib:
    needle = "    fun interface MaaEventCallback : Callback {\n"
    callback = """    fun interface MaaCustomRecognitionCallback : Callback {
        operator fun invoke(
            context: Pointer?, taskId: Long, nodeName: String?,
            customRecognitionName: String?, customRecognitionParam: String?,
            image: Pointer?, roi: Pointer?, transArg: Pointer?,
            outBox: Pointer?, outDetail: Pointer?,
        ): Byte
    }

"""
    if needle not in lib:
        raise SystemExit("cannot find MaaEventCallback declaration")
    lib = lib.replace(needle, callback + needle, 1)
lib_path.write_text(lib, encoding="utf-8")

if "AlasCustomRecognition.NAME" not in runner:
    marker = "            lib.MaaResourceAddSink(res, eventSink, null)\n"
    registration = marker + "            if (lib.MaaResourceRegisterCustomRecognition(res, AlasCustomRecognition.NAME, AlasCustomRecognition.callback, null).toInt() == 0) {\n                lib.MaaResourceDestroy(res)\n                return \"注册 AlasMapRecognition 失败\"\n            }\n"
    if marker not in runner:
        raise SystemExit("cannot find resource sink marker")
    runner = runner.replace(marker, registration, 1)
runner_path.write_text(runner, encoding="utf-8")
PY

printf '[ok] Custom Recognition bridge integrated into %s\n' "$HOST_DIR"
grep -F 'MaaResourceRegisterCustomRecognition' "$LIB" | head -1
grep -F 'AlasCustomRecognition.NAME' "$RUNNER" | head -1

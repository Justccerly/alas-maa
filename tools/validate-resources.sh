#!/usr/bin/env bash
ROOT_DIR="$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)"
RESOURCE_DIR="$ROOT_DIR/resources"

python3 - "$RESOURCE_DIR" <<'PY'
import json
import os
import sys

root = sys.argv[1]
interface_path = os.path.join(root, "interface.json")
errors = []


def load(path):
    try:
        with open(path, encoding="utf-8") as handle:
            return json.load(handle)
    except Exception as exc:
        errors.append(f"{os.path.relpath(path, root)}: invalid JSON: {exc}")
        return {}

interface = load(interface_path)
if interface.get("interface_version") != 2:
    errors.append("interface.json: interface_version must be 2")

for imported in interface.get("import", []):
    path = os.path.normpath(os.path.join(root, imported))
    if not os.path.isfile(path):
        errors.append(f"interface.json: missing import {imported}")

pipeline_nodes = set()
for resource in interface.get("resource", []):
    for relative_path in resource.get("path", []):
        resource_path = os.path.normpath(os.path.join(root, relative_path))
        if not os.path.isdir(resource_path):
            errors.append(f"interface.json: missing resource path {relative_path}")
            continue
        for directory, _, filenames in os.walk(resource_path):
            for filename in filenames:
                if not filename.endswith(".json"):
                    continue
                path = os.path.join(directory, filename)
                document = load(path)
                if os.path.relpath(path, resource_path).startswith("pipeline" + os.sep) or filename == "default_pipeline.json":
                    pipeline_nodes.update(document.keys())

entries = []
for task in interface.get("task", []):
    entries.append(task.get("entry"))
for imported in interface.get("import", []):
    document = load(os.path.normpath(os.path.join(root, imported)))
    entries.extend(task.get("entry") for task in document.get("task", []))

for entry in entries:
    if entry and entry not in pipeline_nodes:
        errors.append(f"task entry {entry!r} has no matching Pipeline node")

map_pipeline_path = os.path.join(root, "resource", "base", "pipeline", "map_action.json")
map_pipeline = load(map_pipeline_path)
recognition_node = map_pipeline.get("AlasMapRecognition", {})
recognition = recognition_node.get("recognition", {}) if isinstance(recognition_node, dict) else {}
recognition_param = recognition.get("param", {}) if isinstance(recognition, dict) else {}
if recognition.get("type") != "Custom" or recognition_param.get("custom_recognition") != "AlasMapRecognition":
    errors.append("map_action.json: AlasMapRecognition must be a Custom Recognition node")
custom_param = recognition_param.get("custom_recognition_param", {})
if not isinstance(custom_param, dict) or not isinstance(custom_param.get("map"), dict):
    errors.append("map_action.json: AlasMapRecognition must include replay map parameters")

if errors:
    for error in errors:
        print(f"[!!] {error}")
    raise SystemExit(1)

print(f"[ok] Project Interface V2 resource package: {len(entries)} task(s), {len(pipeline_nodes)} pipeline node(s)")
print("[ok] AlasMapRecognition Custom Recognition node")
PY

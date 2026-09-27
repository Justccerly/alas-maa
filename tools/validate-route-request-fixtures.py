#!/usr/bin/env python3
"""Validate complete map route request replay fixtures."""

import json
import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
REPLAYS = ROOT / "resources" / "replays"
ENTRY = re.compile(r"^[A-Za-z0-9_.-]+$")


def coordinate(x, y, width, height, label):
    if not isinstance(x, int) or not isinstance(y, int):
        raise ValueError(f"{label} must contain integer x/y")
    if not (0 <= x < width and 0 <= y < height):
        raise ValueError(f"{label} is outside map bounds")


errors = []
paths = sorted(REPLAYS.glob("map-route-request*.json"))
for path in paths:
    try:
        with path.open(encoding="utf-8") as handle:
            request = json.load(handle)
        recognition = request["recognition"]
        width, height = recognition["width"], recognition["height"]
        if not isinstance(width, int) or not isinstance(height, int) or width <= 0 or height <= 0:
            raise ValueError("recognition width and height must be positive integers")
        coordinate(request["fleetX"], request["fleetY"], width, height, "fleet")
        coordinate(request["targetX"], request["targetY"], width, height, "target")
        if not isinstance(request["movementPoints"], int) or request["movementPoints"] < 0:
            raise ValueError("movementPoints must be non-negative")
        for field in ("originX", "originY"):
            if not isinstance(request[field], int):
                raise ValueError(f"{field} must be an integer")
        for field in ("cellWidth", "cellHeight"):
            if not isinstance(request[field], int) or request[field] <= 0:
                raise ValueError(f"{field} must be positive")
        confidence = request.get("minimumConfidence")
        if confidence is not None and (not isinstance(confidence, (int, float)) or not 0 <= confidence <= 1):
            raise ValueError("minimumConfidence must be between 0 and 1")
        entry = request.get("entry", "AlasMapAction")
        if not isinstance(entry, str) or not ENTRY.fullmatch(entry):
            raise ValueError("entry contains unsupported characters")
        cells = recognition["cells"]
        coordinates = set()
        for cell in cells:
            coordinate(cell["x"], cell["y"], width, height, "recognized cell")
            pair = (cell["x"], cell["y"])
            if pair in coordinates:
                raise ValueError("recognized cells contain duplicate coordinates")
            coordinates.add(pair)
        print(f"[ok] {path.relative_to(ROOT)}: {width}x{height}, {len(cells)} cell(s), entry {entry}")
    except (KeyError, TypeError, ValueError, json.JSONDecodeError) as exc:
        errors.append(f"[!!] {path.relative_to(ROOT)}: {exc}")

if errors:
    print("\n".join(errors))
    sys.exit(1)
if not paths:
    print("[info] no route request replay fixtures found")

#!/usr/bin/env python3
"""Validate replay documents consumed by alas-maafw.MapRecognitionJson."""

import json
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
REPLAYS = ROOT / "resources" / "replays"


def coordinate(cell):
    if not isinstance(cell, dict) or not isinstance(cell.get("x"), int) or not isinstance(cell.get("y"), int):
        raise ValueError(f"invalid cell coordinate: {cell!r}")
    return cell["x"], cell["y"]


errors = []
for path in sorted(REPLAYS.glob("*.json")):
    try:
        with path.open(encoding="utf-8") as handle:
            document = json.load(handle)
        width, height = document["width"], document["height"]
        if not isinstance(width, int) or not isinstance(height, int) or width <= 0 or height <= 0:
            raise ValueError("width and height must be positive integers")
        cells = document["cells"]
        if not isinstance(cells, list):
            raise ValueError("cells must be a list")
        coordinates = [coordinate(cell) for cell in cells]
        if len(set(coordinates)) != len(coordinates):
            raise ValueError("cells contain duplicate coordinates")
        if any(not (0 <= x < width and 0 <= y < height) for x, y in coordinates):
            raise ValueError("cell coordinate is outside map bounds")
        for cell in cells:
            confidence = cell.get("confidence")
            if confidence is not None and (not isinstance(confidence, (int, float)) or not 0 <= confidence <= 1):
                raise ValueError("confidence must be between 0 and 1")
            move_cost = cell.get("moveCost")
            if move_cost is not None and (not isinstance(move_cost, int) or move_cost <= 0):
                raise ValueError("moveCost must be positive")
        print(f"[ok] {path.relative_to(ROOT)}: {len(cells)} recognized cell(s)")
    except (KeyError, TypeError, ValueError, json.JSONDecodeError) as exc:
        errors.append(f"[!!] {path.relative_to(ROOT)}: {exc}")

if errors:
    print("\n".join(errors))
    sys.exit(1)
if not list(REPLAYS.glob("*.json")):
    print("[info] no recognition replay fixtures found")

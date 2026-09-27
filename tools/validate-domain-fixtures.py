#!/usr/bin/env python3
"""Validate deterministic map replay fixtures without Android or MaaFramework dependencies."""

import json
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
MAPS = ROOT / "resources" / "maps"


def coordinate(value):
    if not isinstance(value, list) or len(value) != 2 or not all(isinstance(part, int) for part in value):
        raise ValueError(f"invalid coordinate: {value!r}")
    return tuple(value)


def validate(path):
    with path.open(encoding="utf-8") as handle:
        fixture = json.load(handle)
    width = fixture["width"]
    height = fixture["height"]
    if not isinstance(width, int) or not isinstance(height, int) or width <= 0 or height <= 0:
        raise ValueError("width and height must be positive integers")

    start = coordinate(fixture["start"])
    target = coordinate(fixture["target"])
    expected = [coordinate(item) for item in fixture["expectedPath"]]
    blocked = {coordinate(item) for item in fixture.get("blocked", [])}

    def inside(item):
        return 0 <= item[0] < width and 0 <= item[1] < height

    if not all(inside(item) for item in [start, target, *expected, *blocked]):
        raise ValueError("fixture contains a coordinate outside map bounds")
    if expected[0] != start or expected[-1] != target:
        raise ValueError("expectedPath must start at start and end at target")
    if any(item in blocked for item in expected):
        raise ValueError("expectedPath enters a blocked coordinate")
    if any(abs(a[0] - b[0]) + abs(a[1] - b[1]) != 1 for a, b in zip(expected, expected[1:])):
        raise ValueError("expectedPath must use four-direction movement")
    cost = len(expected) - 1
    if fixture["expectedCost"] != cost:
        raise ValueError(f"expectedCost={fixture['expectedCost']} does not match path cost={cost}")
    if cost > fixture["movementPoints"]:
        raise ValueError("expected path exceeds movementPoints")
    return len(expected), cost


errors = []
for path in sorted(MAPS.glob("*.json")):
    try:
        steps, cost = validate(path)
        print(f"[ok] {path.relative_to(ROOT)}: {steps} nodes, cost {cost}")
    except (KeyError, TypeError, ValueError, json.JSONDecodeError) as exc:
        errors.append(f"[!!] {path.relative_to(ROOT)}: {exc}")

if errors:
    print("\n".join(errors))
    sys.exit(1)
if not list(MAPS.glob("*.json")):
    print("[info] no map replay fixtures found")

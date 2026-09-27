#!/usr/bin/env python3
"""Validate action-plan replay fixtures and their expected Pipeline shape."""

import json
import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
REPLAYS = ROOT / "resources" / "replays"
ENTRY = re.compile(r"^[A-Za-z0-9_.-]+$")


def validate(path):
    with path.open(encoding="utf-8") as handle:
        fixture = json.load(handle)
    entry = fixture["entry"]
    if not isinstance(entry, str) or not ENTRY.fullmatch(entry):
        raise ValueError("entry must contain only letters, digits, _, ., or -")
    actions = fixture["actions"]
    if not isinstance(actions, list) or not actions:
        raise ValueError("actions must be a non-empty list")

    coordinates = set()
    for index, action in enumerate(actions):
        if not isinstance(action, dict):
            raise ValueError(f"action {index} must be an object")
        for field in ("x", "y", "coordinateX", "coordinateY"):
            if not isinstance(action.get(field), int):
                raise ValueError(f"action {index}: {field} must be an integer")
        delay = action.get("postDelayMs")
        if not isinstance(delay, int) or delay < 0:
            raise ValueError(f"action {index}: postDelayMs must be non-negative")
        coordinate = (action["coordinateX"], action["coordinateY"])
        if coordinate in coordinates:
            raise ValueError(f"action {index}: duplicate map coordinate")
        coordinates.add(coordinate)

    expected_nodes = [entry] + [f"{entry}.{index}" for index in range(1, len(actions))]
    if expected_nodes[0] != entry:
        raise ValueError("first Pipeline node must equal entry")
    print(f"[ok] {path.relative_to(ROOT)}: {len(actions)} Click node(s): {' -> '.join(expected_nodes)}")


errors = []
for path in sorted(REPLAYS.glob("map-action-*.json")):
    try:
        validate(path)
    except (KeyError, TypeError, ValueError, json.JSONDecodeError) as exc:
        errors.append(f"[!!] {path.relative_to(ROOT)}: {exc}")

if errors:
    print("\n".join(errors))
    sys.exit(1)

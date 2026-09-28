#!/usr/bin/env python3
"""Validate screenshot replay fixtures and their manifests.

A fixture is a recorded device frame plus a manifest describing what it is expected to
contain. The manifest pins the exact bytes via sha256, so a fixture that was re-encoded or
hand-edited is caught here rather than silently changing what the tests assert.

The PNG itself is checked structurally (signature, IHDR dimensions) without a decoding
library, so this validator has no third-party dependency and runs in the CI validate job.
"""

import hashlib
import json
import struct
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
SCREENSHOTS = ROOT / "resources" / "screenshots"

# Screenshots are the one fixture kind that cannot be small. The budget keeps a single
# frame from growing the repository unnoticed, and forces the question at review time.
MAX_PNG_BYTES = 700 * 1024
MAX_TOTAL_BYTES = 4 * 1024 * 1024
PNG_SIGNATURE = b"\x89PNG\r\n\x1a\n"
SHA256_RE = __import__("re").compile(r"^[0-9a-f]{64}$")


def read_png_header(path):
    """Return (width, height, bit_depth, colour_type) from IHDR without decoding pixels."""
    data = path.read_bytes()
    if not data.startswith(PNG_SIGNATURE):
        raise ValueError("not a PNG file (bad signature)")
    if len(data) < 33:
        raise ValueError("truncated PNG: no complete IHDR chunk")
    length, chunk_type = struct.unpack(">I4s", data[8:16])
    if chunk_type != b"IHDR" or length != 13:
        raise ValueError("first PNG chunk is not a 13-byte IHDR")
    width, height, bit_depth, colour_type = struct.unpack(">IIBB", data[16:26])
    return width, height, bit_depth, colour_type, data


def validate(manifest_path):
    with manifest_path.open(encoding="utf-8") as handle:
        manifest = json.load(handle)

    name = manifest.get("name")
    if not isinstance(name, str) or not name:
        raise ValueError("name must be a non-empty string")

    image_name = manifest.get("image")
    if not isinstance(image_name, str) or not image_name:
        raise ValueError("image must be a non-empty string")
    if Path(image_name).name != image_name:
        raise ValueError(f"image must be a bare filename, not a path: {image_name!r}")

    image_path = manifest_path.parent / image_name
    if not image_path.is_file():
        raise ValueError(f"missing image: {image_name}")

    width, height, bit_depth, colour_type, data = read_png_header(image_path)

    if manifest.get("width") != width or manifest.get("height") != height:
        raise ValueError(
            f"manifest size {manifest.get('width')}x{manifest.get('height')} "
            f"does not match PNG {width}x{height}"
        )
    if width <= 0 or height <= 0:
        raise ValueError("dimensions must be positive")

    expected_sha = manifest.get("sha256")
    if not isinstance(expected_sha, str) or not SHA256_RE.match(expected_sha):
        raise ValueError("sha256 must be 64 lowercase hex characters")
    actual_sha = hashlib.sha256(data).hexdigest()
    if actual_sha != expected_sha:
        raise ValueError(
            f"sha256 mismatch: manifest {expected_sha[:16]}... actual {actual_sha[:16]}... "
            "(re-run the fixture generator instead of editing the PNG)"
        )

    if len(data) > MAX_PNG_BYTES:
        raise ValueError(
            f"image is {len(data) / 1024:.0f} KiB, over the {MAX_PNG_BYTES // 1024} KiB budget"
        )

    regions = manifest.get("regions", [])
    if not isinstance(regions, list):
        raise ValueError("regions must be a list")
    seen = set()
    for index, region in enumerate(regions):
        if not isinstance(region, dict):
            raise ValueError(f"region {index} must be an object")
        region_name = region.get("name")
        if not isinstance(region_name, str) or not region_name:
            raise ValueError(f"region {index}: name must be a non-empty string")
        if region_name in seen:
            raise ValueError(f"region {index}: duplicate name {region_name!r}")
        seen.add(region_name)

        box = region.get("box")
        if not isinstance(box, dict):
            raise ValueError(f"region {region_name}: box must be an object")
        for field in ("x", "y", "width", "height"):
            if not isinstance(box.get(field), int):
                raise ValueError(f"region {region_name}: box.{field} must be an integer")
        if box["x"] < 0 or box["y"] < 0 or box["width"] <= 0 or box["height"] <= 0:
            raise ValueError(f"region {region_name}: box must be non-negative with positive size")
        if box["x"] + box["width"] > width or box["y"] + box["height"] > height:
            raise ValueError(f"region {region_name}: box exceeds the {width}x{height} image")

        mean = region.get("meanRgb")
        if not isinstance(mean, list) or len(mean) != 3:
            raise ValueError(f"region {region_name}: meanRgb must be a 3-element list")
        for channel in mean:
            if not isinstance(channel, (int, float)) or not 0 <= channel <= 255:
                raise ValueError(f"region {region_name}: meanRgb channel out of range")

    source = manifest.get("source")
    if source is not None and not isinstance(source, dict):
        raise ValueError("source must be an object when present")

    expected = manifest.get("expected")
    if expected is not None:
        if not isinstance(expected, dict) or not isinstance(expected.get("kind"), str):
            raise ValueError("expected.kind must be a string when expected is present")

    return name, len(data), len(regions), bit_depth, colour_type


errors = []
manifests = sorted(SCREENSHOTS.glob("*.json")) if SCREENSHOTS.is_dir() else []
total = 0

for manifest_path in manifests:
    try:
        name, size, regions, depth, colour = validate(manifest_path)
        total += size
        print(
            f"[ok] {manifest_path.relative_to(ROOT)}: {name}, "
            f"{size / 1024:.0f} KiB, {regions} region(s), {depth}bpp/type{colour}"
        )
    except (KeyError, TypeError, ValueError, json.JSONDecodeError, OSError) as exc:
        errors.append(f"[!!] {manifest_path.relative_to(ROOT)}: {exc}")

if total > MAX_TOTAL_BYTES:
    errors.append(
        f"[!!] screenshot fixtures total {total / 1024 / 1024:.2f} MiB, "
        f"over the {MAX_TOTAL_BYTES / 1024 / 1024:.0f} MiB budget"
    )

if errors:
    print("\n".join(errors))
    sys.exit(1)

if not manifests:
    print("[info] no screenshot fixtures found")
else:
    print(f"[ok] screenshot fixtures total {total / 1024:.0f} KiB")

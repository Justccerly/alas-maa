#!/usr/bin/env python3
"""Generate a screenshot replay fixture from a device capture.

Usage:
    python3 tools/make-screenshot-fixture.py <capture.png> <name> [--kind KIND] [--device DEVICE]

Writes `resources/screenshots/<name>.png` plus `<name>.json`. The frame is quantised to 256
colours so it stays inside the repository budget; recognition cares about the real device
resolution, so the frame is never downscaled.

Region means are computed from the *unquantised* source, and the verifier's tolerance exists
precisely to absorb the difference that quantisation introduces.
"""

import argparse
import hashlib
import json
import sys
from pathlib import Path

try:
    from PIL import Image
except ImportError:
    print("[!!] Pillow is required: python3 -m pip install Pillow", file=sys.stderr)
    sys.exit(2)

ROOT = Path(__file__).resolve().parents[1]
OUT_DIR = ROOT / "resources" / "screenshots"
MAX_PNG_BYTES = 700 * 1024

# Region sets are per-screen-kind because the areas worth asserting differ by screen.
REGION_SETS = {
    "game-download": [
        ("title", (60, 190, 660, 410), "服务器/阵营标题区，后续模板识别目标"),
        ("progress", (0, 660, 1280, 720), "下载进度条区，用于识别“正在下载”状态"),
    ],
    "game-main": [
        ("topBar", (0, 0, 1280, 60), "顶部资源条"),
        ("bottomBar", (0, 660, 1280, 720), "底部功能入口"),
    ],
    "unknown": [],
}


def mean_rgb(image, box):
    region = image.crop(box)
    # tobytes() is the stable API; getdata() is deprecated in Pillow 14.
    raw = region.tobytes()
    count = len(raw) // 3
    return [round(sum(raw[i::3]) / count, 2) for i in range(3)]


def clamp_box(box, width, height):
    x1, y1, x2, y2 = box
    return (max(0, min(x1, width)), max(0, min(y1, height)),
            max(0, min(x2, width)), max(0, min(y2, height)))


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("capture", type=Path, help="source PNG from the device")
    parser.add_argument("name", help="fixture name, e.g. game-download-1280x720")
    parser.add_argument("--kind", default="unknown", choices=sorted(REGION_SETS))
    parser.add_argument("--device", default="MuMu Player 15.0 (Android 15)")
    parser.add_argument("--captured-at", default=None, help="defaults to today")
    parser.add_argument("--note", default=None)
    args = parser.parse_args()

    if not args.capture.is_file():
        print(f"[!!] capture not found: {args.capture}", file=sys.stderr)
        sys.exit(1)
    if "/" in args.name or "\\" in args.name or not args.name:
        print("[!!] name must be a bare filename component", file=sys.stderr)
        sys.exit(1)

    from datetime import date

    source = Image.open(args.capture).convert("RGB")
    OUT_DIR.mkdir(parents=True, exist_ok=True)
    png_path = OUT_DIR / f"{args.name}.png"

    quantised = source.quantize(colors=256, method=Image.MEDIANCUT)
    quantised.save(png_path, optimize=True, compress_level=9)
    size = png_path.stat().st_size

    if size > MAX_PNG_BYTES:
        png_path.unlink(missing_ok=True)
        print(
            f"[!!] {size / 1024:.0f} KiB exceeds the {MAX_PNG_BYTES // 1024} KiB budget; "
            "pick a smaller capture or raise the budget in the validator deliberately",
            file=sys.stderr,
        )
        sys.exit(1)

    regions = []
    for region_name, box, note in REGION_SETS[args.kind]:
        clamped = clamp_box(box, source.width, source.height)
        if clamped[0] >= clamped[2] or clamped[1] >= clamped[3]:
            continue
        regions.append({
            "name": region_name,
            "box": {
                "x": clamped[0], "y": clamped[1],
                "width": clamped[2] - clamped[0], "height": clamped[3] - clamped[1],
            },
            "note": note,
            "meanRgb": mean_rgb(source, clamped),
        })

    manifest = {
        "name": args.name,
        "image": png_path.name,
        "source": {
            "kind": "device-screenshot",
            "device": args.device,
            "capturedAt": args.captured_at or date.today().isoformat(),
            **({"note": args.note} if args.note else {}),
        },
        "width": source.width,
        "height": source.height,
        "sha256": hashlib.sha256(png_path.read_bytes()).hexdigest(),
        "regions": regions,
        "expected": {"kind": args.kind},
    }

    manifest_path = OUT_DIR / f"{args.name}.json"
    manifest_path.write_text(
        json.dumps(manifest, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")

    print(f"[ok] {png_path.relative_to(ROOT)}: {source.width}x{source.height}, {size / 1024:.0f} KiB")
    print(f"[ok] {manifest_path.relative_to(ROOT)}: {len(regions)} region(s), kind={args.kind}")


if __name__ == "__main__":
    main()

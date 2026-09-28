# Resource workspace
This directory is the MaaFramework Project Interface V2 package for Alas Maa.

Current package layout:
- `interface.json`: PI V2 entry point consumed by MaaFwApp
- `tasks/`: user-facing task declarations imported by `interface.json`
- `resource/base/`: runtime resource path selected by the default resource pack
- `resource/base/pipeline/`: MaaFramework Pipeline nodes
- `pipeline/`: reserved authoring area for reusable flows not yet promoted to the runtime package
- `templates/`: screen templates used by recognition nodes
- `maps/`: map definitions and replay fixtures
- `replays/`: recognition JSON replay fixtures consumed by the adapter tests
- `replays/map-action-plan.json`: deterministic action-plan fixture used to validate Pipeline Click chaining
- `replays/map-route-request.json`: complete route-planning request fixture
- `screenshots/`: recorded device frames plus manifests, for recognition replay tests
- `resource/base/pipeline/map_action.json` also declares the
  `AlasMapRecognition` Custom Recognition node used by the host callback bridge.
- `game-data/`: versioned game and task data

## Screenshot replay fixtures

`resources/screenshots/` holds real device captures so recognition can be tested without a
device. Each fixture is a PNG plus a JSON manifest pinning the frame's `sha256`, its device
resolution, and the mean colour of named regions.

Generate one with:

```bash
python3 tools/make-screenshot-fixture.py <capture.png> <name> --kind <game-download|game-main|unknown>
```

`tools/validate-screenshot-fixtures.py` checks the PNG header against the manifest, verifies
the checksum, bounds every region to the frame, and enforces a per-image and total size
budget. Frames are quantised to 256 colours to stay inside that budget and are never
downscaled, because recognition depends on the real device resolution.

A fixture that no longer matches is a deliberate signal to re-record it, not a flaky failure:
either the game or the capture path changed.

`resources/maps/blocked-route.json` is the first deterministic map replay fixture. `alas-domain/` contains the platform-independent Kotlin model and pathfinder that will consume this kind of snapshot.

`tools/validate-resources.sh` checks JSON syntax, PI imports, resource paths, and task-to-Pipeline entry points. Do not place Android UI code or device-control code here.

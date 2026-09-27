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
- `game-data/`: versioned game and task data

`resources/maps/blocked-route.json` is the first deterministic map replay fixture. `alas-domain/` contains the platform-independent Kotlin model and pathfinder that will consume this kind of snapshot.

`tools/validate-resources.sh` checks JSON syntax, PI imports, resource paths, and task-to-Pipeline entry points. Do not place Android UI code or device-control code here.

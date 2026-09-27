# Alas MaaFramework adapter

This module is the boundary between MaaFramework recognition/input data and the
platform-independent `alas-domain` module. It deliberately contains no Android,
Compose, ADB, or native MaaFramework handle types.

Current slice:

- `MapRecognitionAdapter` converts conservative cell observations into
  `RecognizedMap` and then `MapSnapshot`.
- `MapRecognitionJson` decodes replayable JSON emitted by an OCR/template/custom
  recognition bridge, with optional confidence filtering.
- `MapActionPlanner` converts a found domain path into deterministic screen tap
  actions using a supplied grid geometry.
- `MapActionExecutor` sends those actions through a host-provided `TapSink` and
  keeps delay handling injectable for replay tests.
- `MapActionPlanCodec` encodes and decodes the same plan format for host logging
  and replay.
- `MapActionPipelineEncoder` turns a found plan into chained fixed-coordinate
  `Click` nodes for `MaaTaskerPostTask` pipeline overrides.
- `encodeOverrides` returns the `List<JsonObject>` shape expected by the
  MaaFwApp `RuntimeTask` boundary; `encode` remains available for native JSON
  boundaries.
- `MapActionRuntimeSpecFactory` packages the entry and overrides into the exact
  platform-neutral task shape the host needs.
- `MapActionRuntimeSpec.asRuntimeTask` exposes a validated task boundary that
  mirrors the host run plan without importing Android or MaaFwApp classes.
- `MapRoutePlanner` composes recognition replay, A* routing, action planning,
  and runtime task generation in one offline-testable operation.
- `MapRouteRuntimePlanner` converts a route result into a validated
  `MapRuntimeTask`; unreachable routes return a report and never enter the host
  task queue.
- `MapRouteDiagnostics` provides map coverage, blocked/unknown counts, path
  cost, tap count, and target details for logs and failure reports.
- `MapRouteReportCodec` serializes found and unreachable results into stable
  JSON for host logs and replay artifacts.
- `MapRouteRequestCodec` serializes the complete route-planning input so a
  replay does not require test code to reconstruct parameters.
- `MapRouteRequest` rejects invalid coordinates, movement points, grid sizes,
  confidence thresholds, and task entry names at construction time.
- Tests cover duplicate observations, unknown cells, bounds, and action output.

The Android/MaaFramework callback layer can feed these classes after it parses
OCR, template, or custom-recognition callbacks. Device input remains owned by
the MaaFwApp runner.

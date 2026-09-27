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
- Tests cover duplicate observations, unknown cells, bounds, and action output.

The Android/MaaFramework callback layer can feed these classes after it parses
OCR, template, or custom-recognition callbacks. Device input remains owned by
the MaaFwApp runner.

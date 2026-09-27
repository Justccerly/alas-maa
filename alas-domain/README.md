# Alas Domain

This module contains platform-independent game state and decision logic. It must not import Android, Compose, MaaFramework, ADB, or image-recognition APIs.

Current slice:

- `MapModel.kt`: immutable coordinates, grid snapshots, fleet state, and path results
- `MapPathfinder.kt`: deterministic four-direction A* with movement-point limits
- `MapPathfinderTest.kt`: obstacle routing and movement-budget tests

The MaaFramework adapter will later translate recognition results into `MapSnapshot`. The domain returns a path and does not perform clicks or swipes. Those actions belong to the application and MaaFramework layers.

This module is intentionally not wired into `MaaFwApp` yet. The first integration step is to add it as a pure Kotlin module and run these tests on the same CI runner as the Android build.
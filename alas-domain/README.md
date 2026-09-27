# Alas Domain

This module contains platform-independent game state and decision logic. It must not import Android, Compose, MaaFramework, ADB, or image-recognition APIs.
Current slice:
- `MapModel.kt`: immutable coordinates, grid snapshots, fleet state, and path results
- `MapPathfinder.kt`: deterministic four-direction A* with movement-point limits
- `MapRecognition.kt`: conservative conversion from recognized cell semantics to `MapSnapshot`
- `MapPathfinderTest.kt` and `MapSnapshotMapperTest.kt`: routing, budget, partial-observation, and input-validation tests
This is a Kotlin/JVM Gradle module. During the MaaFwApp build job, `tools/integrate-domain-module.sh` copies it into the pinned host, adds `:alas-domain` to `settings.gradle.kts`, adds `implementation(project(":alas-domain"))` to `app`, and runs `:alas-domain:test` before the APK build.
The MaaFramework adapter will later translate OCR/template/custom-recognition output into `RecognizedMap`. The domain mapper returns a `MapSnapshot`; the pathfinder returns a path and does not perform clicks or swipes. Those actions belong to the application and MaaFramework layers.

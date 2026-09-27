# Alas Domain

This module contains platform-independent game state and decision logic. It must not import Android, Compose, MaaFramework, ADB, or image-recognition APIs.

Current slice:
- `MapModel.kt`: immutable coordinates, grid snapshots, fleet state, and path results
- `MapPathfinder.kt`: deterministic four-direction A* with movement-point limits
- `MapPathfinderTest.kt`: obstacle routing and movement-budget tests

This is a Kotlin/JVM Gradle module. During the MaaFwApp build job, `tools/integrate-domain-module.sh` copies it into the pinned host, adds `:alas-domain` to `settings.gradle.kts`, adds `implementation(project(":alas-domain"))` to `app`, and runs `:alas-domain:test` before the APK build.

The MaaFramework adapter will later translate recognition results into `MapSnapshot`. The domain returns a path and does not perform clicks or swipes. Those actions belong to the application and MaaFramework layers.
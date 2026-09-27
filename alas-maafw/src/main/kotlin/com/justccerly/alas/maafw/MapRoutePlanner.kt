package com.justccerly.alas.maafw

import com.justccerly.alas.domain.FleetState
import com.justccerly.alas.domain.GridKind
import com.justccerly.alas.domain.MapCoordinate
import com.justccerly.alas.domain.MapSnapshot
import com.justccerly.alas.domain.MapPathfinder
import com.justccerly.alas.domain.PathResult

sealed interface MapRoutePlanResult {
    data class Found(
        val path: PathResult.Found,
        val runtime: MapActionRuntimeSpec,
        val diagnostics: MapRouteDiagnostics,
    ) : MapRoutePlanResult

    data class Unreachable(
        val result: PathResult.Unreachable,
        val diagnostics: MapRouteDiagnostics,
    ) : MapRoutePlanResult
}

/** Small structured summary suitable for run logs and failure reports. */
data class MapRouteDiagnostics(
    val mapWidth: Int,
    val mapHeight: Int,
    val observedCells: Int,
    val unknownCells: Int,
    val blockedCells: Int,
    val fleetPosition: MapCoordinate,
    val target: MapCoordinate,
    val movementPoints: Int,
    val pathCost: Int? = null,
    val tapCount: Int = 0,
)

/** Orchestrates the offline slice without depending on Android or MaaFramework handles. */
class MapRoutePlanner(
    private val pathfinder: MapPathfinder = MapPathfinder(),
) {
    fun plan(request: MapRouteRequest): MapRoutePlanResult = plan(
        recognitionJson = MapRecognitionJson.encodeDocument(request.recognition),
        fleet = request.fleet(),
        target = request.target(),
        geometry = request.geometry(),
        minimumConfidence = request.minimumConfidence,
        allowEnemyGrid = request.allowEnemyGrid,
        entry = request.entry,
    )

    fun planRequest(document: String): MapRoutePlanResult =
        plan(MapRouteRequestCodec.decode(document))

    fun plan(
        recognitionJson: String,
        fleet: FleetState,
        target: MapCoordinate,
        geometry: MapGridGeometry,
        minimumConfidence: Double? = null,
        allowEnemyGrid: Boolean = false,
        entry: String = "AlasMapAction",
    ): MapRoutePlanResult {
        val snapshot = MapRecognitionJson.decode(recognitionJson, minimumConfidence)
        val baseDiagnostics = snapshot.toDiagnostics(fleet, target)
        return when (val result = pathfinder.findPath(snapshot, fleet, target, allowEnemyGrid)) {
            is PathResult.Found -> MapRoutePlanResult.Found(
                path = result,
                runtime = MapActionRuntimeSpecFactory.fromPlan(
                    MapActionPlanner.plan(result, geometry),
                    entry,
                ),
                diagnostics = baseDiagnostics.copy(
                    pathCost = result.path.totalCost,
                    tapCount = result.path.coordinates.size - 1,
                ),
            )

            is PathResult.Unreachable -> MapRoutePlanResult.Unreachable(result, baseDiagnostics)
        }
    }

    private fun MapSnapshot.toDiagnostics(
        fleet: FleetState,
        target: MapCoordinate,
    ): MapRouteDiagnostics = MapRouteDiagnostics(
        mapWidth = width,
        mapHeight = height,
        observedCells = grids.size,
        unknownCells = grids.count { it.kind == GridKind.UNKNOWN },
        blockedCells = grids.count { !it.traversable },
        fleetPosition = fleet.position,
        target = target,
        movementPoints = fleet.movementPoints,
    )
}

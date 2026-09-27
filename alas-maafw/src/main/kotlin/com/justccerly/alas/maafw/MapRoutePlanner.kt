package com.justccerly.alas.maafw

import com.justccerly.alas.domain.FleetState
import com.justccerly.alas.domain.MapCoordinate
import com.justccerly.alas.domain.MapPathfinder
import com.justccerly.alas.domain.PathResult

sealed interface MapRoutePlanResult {
    data class Found(
        val path: PathResult.Found,
        val runtime: MapActionRuntimeSpec,
    ) : MapRoutePlanResult

    data class Unreachable(
        val result: PathResult.Unreachable,
    ) : MapRoutePlanResult
}

/** Orchestrates the offline slice without depending on Android or MaaFramework handles. */
class MapRoutePlanner(
    private val pathfinder: MapPathfinder = MapPathfinder(),
) {
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
        return when (val result = pathfinder.findPath(snapshot, fleet, target, allowEnemyGrid)) {
            is PathResult.Found -> MapRoutePlanResult.Found(
                path = result,
                runtime = MapActionRuntimeSpecFactory.fromPlan(
                    MapActionPlanner.plan(result, geometry),
                    entry,
                ),
            )

            is PathResult.Unreachable -> MapRoutePlanResult.Unreachable(result)
        }
    }
}

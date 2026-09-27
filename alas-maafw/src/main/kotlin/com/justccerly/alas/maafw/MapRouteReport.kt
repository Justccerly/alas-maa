package com.justccerly.alas.maafw

import com.justccerly.alas.domain.MapCoordinate
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
enum class MapRouteReportStatus {
    FOUND,
    UNREACHABLE,
}

@Serializable
data class MapRouteCoordinate(val x: Int, val y: Int)

@Serializable
data class MapRouteReport(
    val status: MapRouteReportStatus,
    val diagnostics: MapRouteDiagnosticsReport,
    val path: List<MapRouteCoordinate> = emptyList(),
    val reason: String? = null,
)

@Serializable
data class MapRouteDiagnosticsReport(
    val mapWidth: Int,
    val mapHeight: Int,
    val observedCells: Int,
    val unknownCells: Int,
    val blockedCells: Int,
    val fleetPosition: MapRouteCoordinate,
    val target: MapRouteCoordinate,
    val movementPoints: Int,
    val pathCost: Int? = null,
    val tapCount: Int = 0,
)

object MapRouteReportCodec {
    private val json = Json { explicitNulls = false }

    fun encode(result: MapRoutePlanResult): String = json.encodeToString(result.toReport())

    private fun MapRoutePlanResult.toReport(): MapRouteReport = when (this) {
        is MapRoutePlanResult.Found -> MapRouteReport(
            status = MapRouteReportStatus.FOUND,
            diagnostics = diagnostics.toReport(),
            path = path.path.coordinates.map(MapCoordinate::toReport),
        )

        is MapRoutePlanResult.Unreachable -> MapRouteReport(
            status = MapRouteReportStatus.UNREACHABLE,
            diagnostics = diagnostics.toReport(),
            reason = result.reason,
        )
    }

    private fun MapRouteDiagnostics.toReport() = MapRouteDiagnosticsReport(
        mapWidth = mapWidth,
        mapHeight = mapHeight,
        observedCells = observedCells,
        unknownCells = unknownCells,
        blockedCells = blockedCells,
        fleetPosition = fleetPosition.toReport(),
        target = target.toReport(),
        movementPoints = movementPoints,
        pathCost = pathCost,
        tapCount = tapCount,
    )

    private fun MapCoordinate.toReport() = MapRouteCoordinate(x, y)
}

package com.justccerly.alas.maafw

import com.justccerly.alas.domain.FleetState
import com.justccerly.alas.domain.MapCoordinate
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class MapRouteRequest(
    val recognition: MapRecognitionDocument,
    val fleetX: Int,
    val fleetY: Int,
    val movementPoints: Int,
    val targetX: Int,
    val targetY: Int,
    val originX: Int,
    val originY: Int,
    val cellWidth: Int,
    val cellHeight: Int,
    val minimumConfidence: Double? = null,
    val allowEnemyGrid: Boolean = false,
    val entry: String = "AlasMapAction",
) {
    fun fleet() = FleetState(MapCoordinate(fleetX, fleetY), movementPoints)
    fun target() = MapCoordinate(targetX, targetY)
    fun geometry() = MapGridGeometry(ScreenPoint(originX, originY), cellWidth, cellHeight)
}

object MapRouteRequestCodec {
    private val json = Json { explicitNulls = false }

    fun decode(document: String): MapRouteRequest = json.decodeFromString(document)

    fun encode(request: MapRouteRequest): String = json.encodeToString(request)
}

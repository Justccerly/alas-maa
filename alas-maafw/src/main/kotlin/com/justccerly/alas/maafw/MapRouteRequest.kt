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
    init {
        require(fleetX >= 0 && fleetY >= 0) { "fleet coordinates must be non-negative" }
        require(targetX >= 0 && targetY >= 0) { "target coordinates must be non-negative" }
        require(movementPoints >= 0) { "movementPoints must be non-negative" }
        require(cellWidth > 0 && cellHeight > 0) { "cell dimensions must be positive" }
        require(minimumConfidence == null || minimumConfidence in 0.0..1.0) {
            "minimumConfidence must be between 0 and 1"
        }
        require(entry.matches(ENTRY_PATTERN)) {
            "entry must contain only letters, digits, _, ., or -"
        }
    }

    fun fleet() = FleetState(MapCoordinate(fleetX, fleetY), movementPoints)
    fun target() = MapCoordinate(targetX, targetY)
    fun geometry() = MapGridGeometry(ScreenPoint(originX, originY), cellWidth, cellHeight)

    private companion object {
        val ENTRY_PATTERN = Regex("[A-Za-z0-9_.-]+")
    }
}

object MapRouteRequestCodec {
    private val json = Json { explicitNulls = false }

    fun decode(document: String): MapRouteRequest = json.decodeFromString(document)

    fun encode(request: MapRouteRequest): String = json.encodeToString(request)
}

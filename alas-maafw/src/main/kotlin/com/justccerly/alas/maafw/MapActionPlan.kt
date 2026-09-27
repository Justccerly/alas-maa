package com.justccerly.alas.maafw

import com.justccerly.alas.domain.MapCoordinate
import com.justccerly.alas.domain.PathResult
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/** Stable status consumed by the Android/Pipeline action bridge. */
@Serializable
enum class ActionPlanStatus {
    FOUND,
    UNREACHABLE,
}

/** A serializable tap intent. Coordinates are kept beside pixels for logging and replay. */
@Serializable
data class PlannedTap(
    val x: Int,
    val y: Int,
    val coordinateX: Int,
    val coordinateY: Int,
    val postDelayMs: Int,
)

/** Device-independent movement output; the host decides how to inject each tap. */
@Serializable
data class MapActionPlan(
    val status: ActionPlanStatus,
    val totalCost: Int? = null,
    val actions: List<PlannedTap> = emptyList(),
    val reason: String? = null,
) {
    init {
        require(totalCost == null || totalCost >= 0) { "totalCost must be non-negative" }
        require(status == ActionPlanStatus.FOUND || actions.isEmpty()) {
            "unreachable plans cannot contain actions"
        }
        require(status == ActionPlanStatus.UNREACHABLE || reason == null) {
            "found plans cannot contain an unreachable reason"
        }
    }
}

object MapActionPlanCodec {
    private val json = Json { explicitNulls = false }

    fun encode(plan: MapActionPlan): String = json.encodeToString(plan)

    /** Decodes a plan received from a recorder or host boundary. */
    fun decode(document: String): MapActionPlan = json.decodeFromString(document)
}

fun TapAction.toPlannedTap(): PlannedTap = PlannedTap(
    x = point.x,
    y = point.y,
    coordinateX = coordinate.x,
    coordinateY = coordinate.y,
    postDelayMs = postDelayMs,
)

fun PathResult.toActionPlan(geometry: MapGridGeometry): MapActionPlan = when (this) {
    is PathResult.Unreachable -> MapActionPlan(
        status = ActionPlanStatus.UNREACHABLE,
        reason = reason,
    )

    is PathResult.Found -> MapActionPlan(
        status = ActionPlanStatus.FOUND,
        totalCost = path.totalCost,
        actions = path.coordinates.drop(1).map { coordinate ->
            TapAction(coordinate, geometry.centerOf(coordinate)).toPlannedTap()
        },
    )
}

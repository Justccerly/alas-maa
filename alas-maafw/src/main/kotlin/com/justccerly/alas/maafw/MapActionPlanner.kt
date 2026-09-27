package com.justccerly.alas.maafw

import com.justccerly.alas.domain.MapCoordinate
import com.justccerly.alas.domain.MapPath
import com.justccerly.alas.domain.PathResult

data class ScreenPoint(val x: Int, val y: Int)

/** Center and spacing of the game's map cells in screenshot coordinates. */
data class MapGridGeometry(
    val origin: ScreenPoint,
    val cellWidth: Int,
    val cellHeight: Int,
) {
    init {
        require(cellWidth > 0) { "cellWidth must be positive" }
        require(cellHeight > 0) { "cellHeight must be positive" }
    }

    fun centerOf(coordinate: MapCoordinate): ScreenPoint = ScreenPoint(
        x = origin.x + coordinate.x * cellWidth,
        y = origin.y + coordinate.y * cellHeight,
    )
}

/** A device-independent input intent for the MaaFramework action layer. */
data class TapAction(
    val coordinate: MapCoordinate,
    val point: ScreenPoint,
    val postDelayMs: Int = 150,
) {
    init {
        require(postDelayMs >= 0) { "postDelayMs must be non-negative" }
    }
}

object MapActionPlanner {
    /** Creates the stable action document consumed by the host bridge. */
    fun plan(result: PathResult, geometry: MapGridGeometry): MapActionPlan =
        result.toActionPlan(geometry)

    /** Plans taps for movement steps, excluding the fleet's current cell. */
    fun planMovement(result: PathResult, geometry: MapGridGeometry): List<TapAction> = when (result) {
        is PathResult.Unreachable -> emptyList()
        is PathResult.Found -> result.path.toTapActions(geometry)
    }

    private fun MapPath.toTapActions(geometry: MapGridGeometry): List<TapAction> =
        coordinates.drop(1).map { coordinate ->
            TapAction(coordinate = coordinate, point = geometry.centerOf(coordinate))
        }
}

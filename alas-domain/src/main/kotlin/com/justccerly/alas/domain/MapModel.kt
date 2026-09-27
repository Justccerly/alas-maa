package com.justccerly.alas.domain

/** Immutable map coordinate. The origin is the upper-left map cell. */
data class MapCoordinate(val x: Int, val y: Int) {
    init {
        require(x >= 0) { "x must be non-negative" }
        require(y >= 0) { "y must be non-negative" }
    }
}

enum class GridKind {
    SEA,
    LAND,
    OBSTACLE,
    UNKNOWN,
}

/** A grid snapshot produced by the MaaFramework adapter or a replay fixture. */
data class MapGrid(
    val coordinate: MapCoordinate,
    val kind: GridKind = GridKind.SEA,
    val traversable: Boolean = true,
    val moveCost: Int = 1,
    val occupiedByEnemy: Boolean = false,
) {
    init {
        require(moveCost > 0) { "moveCost must be positive" }
    }
}

data class FleetState(
    val position: MapCoordinate,
    val movementPoints: Int,
    val fleetIndex: Int = 1,
) {
    init {
        require(movementPoints >= 0) { "movementPoints must be non-negative" }
        require(fleetIndex > 0) { "fleetIndex must be positive" }
    }
}

data class MapSnapshot(
    val width: Int,
    val height: Int,
    val grids: List<MapGrid>,
) {
    init {
        require(width > 0) { "width must be positive" }
        require(height > 0) { "height must be positive" }
        require(grids.map { it.coordinate }.toSet().size == grids.size) {
            "grid coordinates must be unique"
        }
        require(grids.all { it.coordinate.x < width && it.coordinate.y < height }) {
            "grid coordinate is outside map bounds"
        }
    }

    private val byCoordinate: Map<MapCoordinate, MapGrid> = grids.associateBy { it.coordinate }

    fun gridAt(coordinate: MapCoordinate): MapGrid? = byCoordinate[coordinate]

    fun neighbors(coordinate: MapCoordinate): List<MapGrid> = buildList {
        if (coordinate.y > 0) add(MapCoordinate(coordinate.x, coordinate.y - 1))
        if (coordinate.x > 0) add(MapCoordinate(coordinate.x - 1, coordinate.y))
        if (coordinate.x + 1 < width) add(MapCoordinate(coordinate.x + 1, coordinate.y))
        if (coordinate.y + 1 < height) add(MapCoordinate(coordinate.x, coordinate.y + 1))
    }.mapNotNull(::gridAt)
}

data class MapPath(
    val coordinates: List<MapCoordinate>,
    val totalCost: Int,
) {
    init {
        require(coordinates.isNotEmpty()) { "path must contain at least one coordinate" }
        require(totalCost >= 0) { "totalCost must be non-negative" }
    }
}

sealed interface PathResult {
    data class Found(val path: MapPath) : PathResult
    data class Unreachable(val reason: String) : PathResult
}

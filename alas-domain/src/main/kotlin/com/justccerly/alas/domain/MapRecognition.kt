package com.justccerly.alas.domain

/**
 * Platform-neutral semantic result for one recognized map cell.
 *
 * The MaaFramework adapter is responsible for turning OCR/template/custom-recognition output into
 * this value. Optional fields allow a recognizer to provide only what it can establish; the mapper
 * fills missing values conservatively so an unknown cell is never treated as traversable by
 * accident.
 */
data class RecognizedGrid(
    val coordinate: MapCoordinate,
    val kind: GridKind = GridKind.UNKNOWN,
    val traversable: Boolean? = null,
    val moveCost: Int? = null,
    val occupiedByEnemy: Boolean = false,
) {
    init {
        require(moveCost == null || moveCost > 0) { "moveCost must be positive when provided" }
    }
}

/** A complete or partial semantic map observation produced by the recognition layer. */
data class RecognizedMap(
    val width: Int,
    val height: Int,
    val cells: List<RecognizedGrid>,
) {
    init {
        require(width > 0) { "width must be positive" }
        require(height > 0) { "height must be positive" }
    }
}

/**
 * Converts recognition semantics into the domain's immutable map model.
 *
 * Missing cells are intentionally not synthesized: a partial observation remains partial and the
 * pathfinder will not walk through absent cells. Unknown recognized cells are mapped to
 * non-traversable cells unless the recognizer explicitly supplies `traversable = true`.
 */
object MapSnapshotMapper {
    fun toSnapshot(observation: RecognizedMap): MapSnapshot {
        val coordinates = observation.cells.map { it.coordinate }
        require(coordinates.toSet().size == coordinates.size) {
            "recognized map contains duplicate cell coordinates"
        }
        require(coordinates.all { it.x in 0 until observation.width && it.y in 0 until observation.height }) {
            "recognized cell coordinate is outside map bounds"
        }
        return MapSnapshot(
            width = observation.width,
            height = observation.height,
            grids = observation.cells.map(::toGrid),
        )
    }

    private fun toGrid(cell: RecognizedGrid): MapGrid = MapGrid(
        coordinate = cell.coordinate,
        kind = cell.kind,
        traversable = cell.traversable ?: defaultTraversable(cell.kind),
        moveCost = cell.moveCost ?: 1,
        occupiedByEnemy = cell.occupiedByEnemy,
    )

    private fun defaultTraversable(kind: GridKind): Boolean = when (kind) {
        GridKind.SEA -> true
        GridKind.LAND,
        GridKind.OBSTACLE,
        GridKind.UNKNOWN -> false
    }
}

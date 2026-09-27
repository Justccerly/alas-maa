package com.justccerly.alas.maafw

import com.justccerly.alas.domain.GridKind
import com.justccerly.alas.domain.MapCoordinate
import com.justccerly.alas.domain.MapSnapshot
import com.justccerly.alas.domain.MapSnapshotMapper
import com.justccerly.alas.domain.RecognizedGrid
import com.justccerly.alas.domain.RecognizedMap

/** A semantic cell observation produced by an OCR/template/custom recognizer. */
data class CellObservation(
    val coordinate: MapCoordinate,
    val kind: GridKind = GridKind.UNKNOWN,
    val traversable: Boolean? = null,
    val moveCost: Int? = null,
    val occupiedByEnemy: Boolean = false,
    val confidence: Double? = null,
) {
    init {
        require(confidence == null || confidence in 0.0..1.0) {
            "confidence must be between 0 and 1"
        }
    }
}

/** Converts adapter observations into the domain's immutable map snapshot. */
object MapRecognitionAdapter {
    fun toSnapshot(width: Int, height: Int, cells: List<CellObservation>): MapSnapshot {
        val recognized = RecognizedMap(
            width = width,
            height = height,
            cells = cells.map { cell ->
                RecognizedGrid(
                    coordinate = cell.coordinate,
                    kind = cell.kind,
                    traversable = cell.traversable,
                    moveCost = cell.moveCost,
                    occupiedByEnemy = cell.occupiedByEnemy,
                )
            },
        )
        return MapSnapshotMapper.toSnapshot(recognized)
    }
}

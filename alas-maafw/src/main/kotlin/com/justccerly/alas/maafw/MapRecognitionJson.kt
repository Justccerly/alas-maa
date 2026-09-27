package com.justccerly.alas.maafw

import com.justccerly.alas.domain.GridKind
import com.justccerly.alas.domain.MapCoordinate
import com.justccerly.alas.domain.MapSnapshot
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/** Stable replay format for OCR/template/custom-recognition output. */
@Serializable
data class MapRecognitionDocument(
    val width: Int,
    val height: Int,
    val cells: List<MapRecognitionCell>,
) {
    init {
        require(width > 0 && height > 0) { "map dimensions must be positive" }
        val coordinates = cells.map { it.x to it.y }
        require(coordinates.toSet().size == coordinates.size) {
            "recognized map contains duplicate cell coordinates"
        }
        require(cells.all { it.x in 0 until width && it.y in 0 until height }) {
            "recognized cell coordinate is outside map bounds"
        }
    }
}

@Serializable
data class MapRecognitionCell(
    val x: Int,
    val y: Int,
    val kind: String? = null,
    val traversable: Boolean? = null,
    val moveCost: Int? = null,
    val occupiedByEnemy: Boolean = false,
    val confidence: Double? = null,
) {
    init {
        require(x >= 0 && y >= 0) { "recognized cell coordinates must be non-negative" }
        require(moveCost == null || moveCost > 0) { "moveCost must be positive when provided" }
        require(confidence == null || confidence in 0.0..1.0) {
            "confidence must be between 0 and 1"
        }
    }
}

/** Decodes MaaFramework adapter output without exposing JSON to the domain layer. */
object MapRecognitionJson {
    private val json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
    }

    fun decode(document: String, minimumConfidence: Double? = null): MapSnapshot {
        require(minimumConfidence == null || minimumConfidence in 0.0..1.0) {
            "minimumConfidence must be between 0 and 1"
        }
        val payload = json.decodeFromString<MapRecognitionDocument>(document)
        val cells = payload.cells
            .filter { it.confidence == null || minimumConfidence == null || it.confidence >= minimumConfidence }
            .map { cell ->
                CellObservation(
                    coordinate = MapCoordinate(cell.x, cell.y),
                    kind = parseKind(cell.kind),
                    traversable = cell.traversable,
                    moveCost = cell.moveCost,
                    occupiedByEnemy = cell.occupiedByEnemy,
                    confidence = cell.confidence,
                )
            }
        return MapRecognitionAdapter.toSnapshot(payload.width, payload.height, cells)
    }

    fun decodeDocument(document: String): MapRecognitionDocument =
        json.decodeFromString(document)

    fun encodeDocument(document: MapRecognitionDocument): String =
        json.encodeToString(document)

    private fun parseKind(raw: String?): GridKind = when (raw?.trim()?.lowercase()) {
        "sea", "water" -> GridKind.SEA
        "land" -> GridKind.LAND
        "obstacle", "blocked" -> GridKind.OBSTACLE
        null, "", "unknown" -> GridKind.UNKNOWN
        else -> GridKind.UNKNOWN
    }
}

package com.justccerly.alas.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertTrue

class MapSnapshotMapperTest {
    @Test
    fun mapsRecognizedCellsAndUsesConservativeUnknownDefaults() {
        val snapshot = MapSnapshotMapper.toSnapshot(
            RecognizedMap(
                width = 3,
                height = 1,
                cells = listOf(
                    RecognizedGrid(MapCoordinate(0, 0), kind = GridKind.SEA),
                    RecognizedGrid(MapCoordinate(1, 0)),
                    RecognizedGrid(
                        MapCoordinate(2, 0),
                        kind = GridKind.SEA,
                        moveCost = 2,
                        occupiedByEnemy = true,
                    ),
                ),
            ),
        )

        assertTrue(snapshot.gridAt(MapCoordinate(0, 0))!!.traversable)
        assertEquals(GridKind.UNKNOWN, snapshot.gridAt(MapCoordinate(1, 0))!!.kind)
        assertFalse(snapshot.gridAt(MapCoordinate(1, 0))!!.traversable)
        assertEquals(2, snapshot.gridAt(MapCoordinate(2, 0))!!.moveCost)
        assertTrue(snapshot.gridAt(MapCoordinate(2, 0))!!.occupiedByEnemy)
    }

    @Test
    fun preservesPartialObservationsWithoutInventingCells() {
        val snapshot = MapSnapshotMapper.toSnapshot(
            RecognizedMap(
                width = 2,
                height = 2,
                cells = listOf(RecognizedGrid(MapCoordinate(0, 0), kind = GridKind.SEA)),
            ),
        )

        assertEquals(1, snapshot.grids.size)
        assertEquals(null, snapshot.gridAt(MapCoordinate(1, 1)))
        assertIs<PathResult.Unreachable>(
            MapPathfinder().findPath(
                snapshot,
                FleetState(MapCoordinate(0, 0), movementPoints = 3),
                MapCoordinate(1, 1),
            ),
        )
    }

    @Test
    fun rejectsDuplicateAndOutOfBoundsRecognizedCoordinates() {
        val duplicate = RecognizedMap(
            width = 2,
            height = 1,
            cells = listOf(
                RecognizedGrid(MapCoordinate(0, 0), kind = GridKind.SEA),
                RecognizedGrid(MapCoordinate(0, 0), kind = GridKind.OBSTACLE),
            ),
        )
        assertFailsWith<IllegalArgumentException> { MapSnapshotMapper.toSnapshot(duplicate) }

        val outside = RecognizedMap(
            width = 1,
            height = 1,
            cells = listOf(RecognizedGrid(MapCoordinate(1, 0), kind = GridKind.SEA)),
        )
        assertFailsWith<IllegalArgumentException> { MapSnapshotMapper.toSnapshot(outside) }
    }
}

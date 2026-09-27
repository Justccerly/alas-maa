package com.justccerly.alas.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class MapPathfinderTest {
    @Test
    fun routesAroundAnObstacleWithDeterministicTieBreaking() {
        val snapshot = MapSnapshot(
            width = 5,
            height = 3,
            grids = (0 until 3).flatMap { y ->
                (0 until 5).map { x ->
                    MapGrid(
                        coordinate = MapCoordinate(x, y),
                        kind = if (x == 2 && y == 1) GridKind.OBSTACLE else GridKind.SEA,
                        traversable = !(x == 2 && y == 1),
                    )
                }
            },
        )

        val result = MapPathfinder().findPath(
            snapshot = snapshot,
            fleet = FleetState(MapCoordinate(0, 1), movementPoints = 6),
            target = MapCoordinate(4, 1),
        )

        val found = assertIs<PathResult.Found>(result)
        assertEquals(6, found.path.totalCost)
        assertEquals(
            listOf(
                MapCoordinate(0, 1),
                MapCoordinate(0, 0),
                MapCoordinate(1, 0),
                MapCoordinate(2, 0),
                MapCoordinate(3, 0),
                MapCoordinate(4, 0),
                MapCoordinate(4, 1),
            ),
            found.path.coordinates,
        )
    }

    @Test
    fun rejectsAPathOverTheFleetMovementBudget() {
        val snapshot = MapSnapshot(
            width = 3,
            height = 1,
            grids = (0 until 3).map { x -> MapGrid(MapCoordinate(x, 0)) },
        )

        val result = MapPathfinder().findPath(
            snapshot = snapshot,
            fleet = FleetState(MapCoordinate(0, 0), movementPoints = 1),
            target = MapCoordinate(2, 0),
        )

        assertIs<PathResult.Unreachable>(result)
    }
}
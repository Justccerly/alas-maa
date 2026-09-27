package com.justccerly.alas.maafw

import com.justccerly.alas.domain.FleetState
import com.justccerly.alas.domain.GridKind
import com.justccerly.alas.domain.MapCoordinate
import com.justccerly.alas.domain.MapGrid
import com.justccerly.alas.domain.MapPathfinder
import com.justccerly.alas.domain.MapSnapshot
import com.justccerly.alas.domain.PathResult
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class MapAdapterTest {
    @Test
    fun convertsRecognitionSemanticsToAUsableSnapshot() {
        val snapshot = MapRecognitionAdapter.toSnapshot(
            width = 2,
            height = 1,
            cells = listOf(
                CellObservation(MapCoordinate(0, 0), kind = GridKind.SEA, confidence = 0.98),
                CellObservation(MapCoordinate(1, 0), kind = GridKind.UNKNOWN),
            ),
        )

        assertTrue(snapshot.gridAt(MapCoordinate(0, 0))!!.traversable)
        assertFalse(snapshot.gridAt(MapCoordinate(1, 0))!!.traversable)
    }

    @Test
    fun plansOnlyMovementStepsAtGridCenters() {
        val snapshot = MapSnapshot(
            width = 3,
            height = 1,
            grids = (0 until 3).map { x -> MapGrid(MapCoordinate(x, 0)) },
        )
        val result = MapPathfinder().findPath(
            snapshot = snapshot,
            fleet = FleetState(MapCoordinate(0, 0), movementPoints = 2),
            target = MapCoordinate(2, 0),
        )

        val actions = MapActionPlanner.planMovement(
            result,
            MapGridGeometry(origin = ScreenPoint(100, 200), cellWidth = 50, cellHeight = 40),
        )

        assertEquals(
            listOf(
                TapAction(MapCoordinate(1, 0), ScreenPoint(150, 200)),
                TapAction(MapCoordinate(2, 0), ScreenPoint(200, 200)),
            ),
            actions,
        )
    }

    @Test
    fun doesNotCreateInputForUnreachablePath() {
        val actions = MapActionPlanner.planMovement(
            PathResult.Unreachable("unknown target"),
            MapGridGeometry(ScreenPoint(0, 0), 10, 10),
        )

        assertTrue(actions.isEmpty())
    }

    @Test
    fun emitsAStableSerializableActionPlan() {
        val result = PathResult.Found(
            com.justccerly.alas.domain.MapPath(
                coordinates = listOf(MapCoordinate(0, 0), MapCoordinate(1, 0)),
                totalCost = 1,
            ),
        )
        val plan = MapActionPlanner.plan(
            result,
            MapGridGeometry(ScreenPoint(10, 20), cellWidth = 30, cellHeight = 40),
        )

        assertEquals(ActionPlanStatus.FOUND, plan.status)
        assertEquals(1, plan.totalCost)
        assertEquals(1, plan.actions.size)
        assertEquals(
            "{\"status\":\"FOUND\",\"totalCost\":1,\"actions\":[{\"x\":40,\"y\":20,\"coordinateX\":1,\"coordinateY\":0,\"postDelayMs\":150}]}",
            MapActionPlanCodec.encode(plan),
        )
    }
}

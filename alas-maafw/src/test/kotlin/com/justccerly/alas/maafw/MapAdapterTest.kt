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
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

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

    @Test
    fun executorSendsTapsInPlanOrderAndHonorsDelays() {
        val points = mutableListOf<ScreenPoint>()
        val delays = mutableListOf<Int>()
        val plan = MapActionPlan(
            status = ActionPlanStatus.FOUND,
            totalCost = 2,
            actions = listOf(
                PlannedTap(10, 20, 1, 0, 25),
                PlannedTap(30, 40, 2, 0, 50),
            ),
        )

        val result = MapActionExecutor(
            tapSink = TapSink { points += it },
            delay = ActionDelay { delays += it },
        ).execute(plan)

        assertEquals(ActionExecutionResult(ActionPlanStatus.FOUND, 2), result)
        assertEquals(listOf(ScreenPoint(10, 20), ScreenPoint(30, 40)), points)
        assertEquals(listOf(25, 50), delays)
    }

    @Test
    fun executorDoesNotTouchAnUnreachablePlan() {
        var tapCount = 0
        val result = MapActionExecutor(TapSink { tapCount += 1 }).execute(
            MapActionPlan(
                status = ActionPlanStatus.UNREACHABLE,
                reason = "missing target",
            ),
        )

        assertEquals(ActionExecutionResult(ActionPlanStatus.UNREACHABLE, 0, "missing target"), result)
        assertEquals(0, tapCount)
    }

    @Test
    fun actionPlanCodecRoundTripsAPlan() {
        val original = MapActionPlan(
            status = ActionPlanStatus.UNREACHABLE,
            reason = "target not observed",
        )

        assertEquals(original, MapActionPlanCodec.decode(MapActionPlanCodec.encode(original)))
    }

    @Test
    fun encodesAPlanAsChainedFixedCoordinateClicks() {
        val plan = MapActionPlan(
            status = ActionPlanStatus.FOUND,
            totalCost = 2,
            actions = listOf(
                PlannedTap(10, 20, 1, 0, 25),
                PlannedTap(30, 40, 2, 0, 50),
            ),
        )

        val document = MapActionPipelineEncoder.encode(plan, entry = "AlasMapAction")

        assertEquals(
            "{\"AlasMapAction\":{\"action\":\"Click\",\"target\":[10,20],\"post_delay\":25,\"next\":[\"AlasMapAction.1\"]},\"AlasMapAction.1\":{\"action\":\"Click\",\"target\":[30,40],\"post_delay\":50}}",
            document,
        )
    }

    @Test
    fun exposesAnOrderedOverrideListForTheHostRuntime() {
        val plan = MapActionPlan(
            status = ActionPlanStatus.FOUND,
            actions = listOf(PlannedTap(10, 20, 1, 0, 25)),
        )

        val overrides = MapActionPipelineEncoder.encodeOverrides(plan)

        assertEquals(1, overrides.size)
        assertEquals(
            "Click",
            overrides.single()["AlasMapAction"]!!.jsonObject["action"]!!.jsonPrimitive.content,
        )
    }

    @Test
    fun createsTheRuntimeTaskShapeExpectedByMaaFwApp() {
        val spec = MapActionRuntimeSpecFactory.fromPlan(
            MapActionPlan(
                status = ActionPlanStatus.FOUND,
                actions = listOf(PlannedTap(10, 20, 1, 0, 25)),
            ),
        )

        assertEquals("AlasMapAction", spec.entry)
        assertEquals(1, spec.pipelineOverrides.size)
        assertEquals("Click", spec.pipelineOverrides.single()["AlasMapAction"]!!.jsonObject["action"]!!.jsonPrimitive.content)
        assertEquals(
            MapRuntimeTask("地图动作计划", "AlasMapAction", spec.pipelineOverrides),
            spec.asRuntimeTask("地图动作计划"),
        )
    }

    @Test
    fun rejectsUnreachablePlansAndUnsafeEntries() {
        val unreachable = MapActionPlan(ActionPlanStatus.UNREACHABLE, reason = "no route")
        kotlin.test.assertFailsWith<IllegalArgumentException> {
            MapActionPipelineEncoder.encode(unreachable)
        }
        kotlin.test.assertFailsWith<IllegalArgumentException> {
            MapActionPipelineEncoder.encode(
                MapActionPlan(ActionPlanStatus.FOUND),
                entry = "Alas Map",
            )
        }
    }
}

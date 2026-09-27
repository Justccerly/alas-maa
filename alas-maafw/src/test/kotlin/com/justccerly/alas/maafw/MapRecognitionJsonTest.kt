package com.justccerly.alas.maafw

import com.justccerly.alas.domain.GridKind
import com.justccerly.alas.domain.MapCoordinate
import com.justccerly.alas.domain.FleetState
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.test.assertIs

class MapRecognitionJsonTest {
    @Test
    fun decodesRecognizedCellsAndIgnoresUnknownFields() {
        val snapshot = MapRecognitionJson.decode(
            """
            {
              "width": 3,
              "height": 1,
              "cells": [
                {"x":0,"y":0,"kind":"sea","confidence":0.99},
                {"x":1,"y":0,"kind":"mystery","confidence":0.80,"future":true},
                {"x":2,"y":0,"kind":"blocked","traversable":false}
              ]
            }
            """.trimIndent(),
        )

        assertEquals(GridKind.SEA, snapshot.gridAt(MapCoordinate(0, 0))!!.kind)
        assertEquals(GridKind.UNKNOWN, snapshot.gridAt(MapCoordinate(1, 0))!!.kind)
        assertTrue(snapshot.gridAt(MapCoordinate(0, 0))!!.traversable)
    }

    @Test
    fun confidenceFilterKeepsPartialObservationConservative() {
        val snapshot = MapRecognitionJson.decode(
            """
            {"width":2,"height":1,"cells":[
              {"x":0,"y":0,"kind":"sea","confidence":0.4},
              {"x":1,"y":0,"kind":"sea","confidence":0.95}
            ]}
            """.trimIndent(),
            minimumConfidence = 0.9,
        )

        assertNull(snapshot.gridAt(MapCoordinate(0, 0)))
        assertTrue(snapshot.gridAt(MapCoordinate(1, 0))!!.traversable)
    }

    @Test
    fun routePlannerBuildsRuntimeSpecFromReplayJson() {
        val json = """
            {"width":3,"height":1,"cells":[
              {"x":0,"y":0,"kind":"sea","confidence":0.99},
              {"x":1,"y":0,"kind":"sea","confidence":0.98},
              {"x":2,"y":0,"kind":"sea","confidence":0.97}
            ]}
        """.trimIndent()

        val result = MapRoutePlanner().plan(
            recognitionJson = json,
            fleet = FleetState(MapCoordinate(0, 0), movementPoints = 2),
            target = MapCoordinate(2, 0),
            geometry = MapGridGeometry(ScreenPoint(100, 200), 50, 40),
        )

        val found = assertIs<MapRoutePlanResult.Found>(result)
        assertEquals(2, found.path.path.totalCost)
        assertEquals("AlasMapAction", found.runtime.entry)
        assertEquals(1, found.runtime.pipelineOverrides.size)
        assertEquals(3, found.diagnostics.observedCells)
        assertEquals(2, found.diagnostics.pathCost)
        assertEquals(2, found.diagnostics.tapCount)
    }

    @Test
    fun routePlannerReturnsUnreachableWhenConfidenceFilterRemovesRoute() {
        val json = """
            {"width":2,"height":1,"cells":[
              {"x":0,"y":0,"kind":"sea","confidence":0.99},
              {"x":1,"y":0,"kind":"sea","confidence":0.30}
            ]}
        """.trimIndent()

        val result = MapRoutePlanner().plan(
            recognitionJson = json,
            fleet = FleetState(MapCoordinate(0, 0), movementPoints = 2),
            target = MapCoordinate(1, 0),
            geometry = MapGridGeometry(ScreenPoint(0, 0), 10, 10),
            minimumConfidence = 0.9,
        )

        val unreachable = assertIs<MapRoutePlanResult.Unreachable>(result)
        assertEquals(1, unreachable.diagnostics.observedCells)
        assertEquals(2, unreachable.diagnostics.movementPoints)
    }

    @Test
    fun routeReportCodecProducesStableSuccessJson() {
        val json = """
            {"width":2,"height":1,"cells":[
              {"x":0,"y":0,"kind":"sea"},
              {"x":1,"y":0,"kind":"sea"}
            ]}
        """.trimIndent()
        val result = MapRoutePlanner().plan(
            json,
            FleetState(MapCoordinate(0, 0), movementPoints = 1),
            MapCoordinate(1, 0),
            MapGridGeometry(ScreenPoint(10, 20), 30, 40),
        )

        assertEquals(
            "{\"status\":\"FOUND\",\"diagnostics\":{\"mapWidth\":2,\"mapHeight\":1,\"observedCells\":2,\"unknownCells\":0,\"blockedCells\":0,\"fleetPosition\":{\"x\":0,\"y\":0},\"target\":{\"x\":1,\"y\":0},\"movementPoints\":1,\"pathCost\":1,\"tapCount\":1},\"path\":[{\"x\":0,\"y\":0},{\"x\":1,\"y\":0}]}"
            MapRouteReportCodec.encode(result),
        )
    }
}

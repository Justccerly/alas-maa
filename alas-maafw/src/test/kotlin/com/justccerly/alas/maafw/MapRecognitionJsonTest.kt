package com.justccerly.alas.maafw

import com.justccerly.alas.domain.GridKind
import com.justccerly.alas.domain.MapCoordinate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

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
}

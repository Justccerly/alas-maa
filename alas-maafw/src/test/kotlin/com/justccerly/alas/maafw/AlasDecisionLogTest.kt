package com.justccerly.alas.maafw

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** Captures decisions in order so assertions can check both content and sequence. */
private class RecordingSink : AlasDecisionSink {
    val decisions = mutableListOf<AlasDecision>()
    override fun record(decision: AlasDecision) {
        decisions += decision
    }
}

class AlasDecisionLogTest {
    private val at = 1_700_000_000_000L

    private fun request(
        width: Int = 3,
        height: Int = 2,
        cells: List<MapRecognitionCell> = listOf(
            MapRecognitionCell(x = 0, y = 0, kind = "sea", confidence = 0.99),
            MapRecognitionCell(x = 1, y = 0, kind = "obstacle", confidence = 0.95),
        ),
        box: MaaRecognitionBox? = MaaRecognitionBox(10, 20, 300, 200),
    ) = MaaMapRecognitionCallbackCodec.Request(
        map = MapRecognitionDocument(width = width, height = height, cells = cells),
        box = box,
    )

    @Test
    fun requestRecordsParameterSizeAndHandlePresence() {
        val sink = RecordingSink()
        val param = """{"map":{"width":3,"height":2,"cells":[]}}"""
        AlasRecognitionDecisionLog.request(
            sink = sink,
            atMillis = at,
            paramJson = param,
            roiPresent = true,
            imagePresent = true,
        )

        val decision = sink.decisions.single()
        assertEquals(AlasDecisionKind.RECOGNITION_REQUEST, decision.kind)
        // Derived from the input so the assertion cannot drift from the payload.
        assertTrue(decision.summary.contains("paramBytes=${param.length}"), decision.summary)
        assertTrue(decision.summary.contains("roi=true"), decision.summary)
        assertTrue(decision.summary.contains("image=true"), decision.summary)
        // The raw parameter is kept so a failing decode can be diagnosed after the fact.
        assertEquals(param, decision.detail)
    }

    @Test
    fun requestWithMissingParametersStillRecords() {
        val sink = RecordingSink()
        AlasRecognitionDecisionLog.request(sink, at, paramJson = null, roiPresent = false, imagePresent = false)

        val decision = sink.decisions.single()
        assertTrue(decision.summary.contains("paramBytes=0"), decision.summary)
        assertTrue(decision.summary.contains("roi=false"), decision.summary)
        assertNull(decision.detail, "no parameter means no detail payload")
    }

    @Test
    fun resultRecordsMapShapeAndBox() {
        val sink = RecordingSink()
        AlasRecognitionDecisionLog.result(
            sink = sink,
            atMillis = at,
            request = request(),
            box = MaaRecognitionBox(10, 20, 300, 200),
        )

        val decision = sink.decisions.single()
        assertEquals(AlasDecisionKind.RECOGNITION_RESULT, decision.kind)
        assertTrue(decision.summary.contains("map=3x2"), decision.summary)
        assertTrue(decision.summary.contains("cells=2"), decision.summary)
        assertTrue(decision.summary.contains("box=10,20,300,200"), decision.summary)
        val detail = decision.detail
        assertTrue(detail != null && detail.contains("\"width\":3"), "$detail")
    }

    @Test
    fun resultReportsConfidenceFilteringWhenProvided() {
        val sink = RecordingSink()
        AlasRecognitionDecisionLog.result(
            sink = sink,
            atMillis = at,
            request = request(),
            box = MaaRecognitionBox(0, 0, 1, 1),
            filteredCells = 1,
        )

        assertTrue(sink.decisions.single().summary.contains("kept=1"))
    }

    @Test
    fun failureKeepsTheReason() {
        val sink = RecordingSink()
        AlasRecognitionDecisionLog.failure(sink, at, IllegalStateException("out_box is null"))

        val decision = sink.decisions.single()
        assertEquals(AlasDecisionKind.RECOGNITION_FAILURE, decision.kind)
        assertTrue(decision.summary.contains("IllegalStateException"), decision.summary)
        assertTrue(decision.summary.contains("out_box is null"), decision.summary)
    }

    @Test
    fun routeKeepsTheReportAsDetail() {
        val sink = RecordingSink()
        val report = """{"status":"UNREACHABLE","reason":"no path"}"""
        AlasRecognitionDecisionLog.route(sink, at, report)

        val decision = sink.decisions.single()
        assertEquals(AlasDecisionKind.ROUTE_PLAN, decision.kind)
        assertEquals(report, decision.detail)
    }

    @Test
    fun longSummaryIsBoundedButStillPrefixed() {
        val sink = RecordingSink()
        AlasRecognitionDecisionLog.failure(sink, at, IllegalStateException("x".repeat(500)))

        val summary = sink.decisions.single().summary
        assertTrue(summary.length <= 203, "length=${summary.length}")
        assertTrue(summary.startsWith("recognition failed: "), summary)
        assertTrue(summary.endsWith("..."), summary)
    }

    @Test
    fun multilineReasonsAreFoldedToOneLine() {
        val sink = RecordingSink()
        AlasRecognitionDecisionLog.failure(sink, at, IllegalStateException("first\nsecond"))

        val summary = sink.decisions.single().summary
        assertTrue(!summary.contains('\n'), summary)
        assertTrue(summary.contains("first second"), summary)
    }

    @Test
    fun blankSummaryIsRejected() {
        // A decision with no text would be a silent hole in the log.
        assertFailsWith<IllegalArgumentException> {
            AlasDecision(atMillis = at, kind = AlasDecisionKind.ROUTE_PLAN, summary = "   ")
        }
    }

    @Test
    fun discardingSinkIsUsableWithoutLogging() {
        // The default sink must be safe to call so callers can wire logging later.
        AlasDecisionSink.Discarding.record(
            AlasDecision(at, AlasDecisionKind.ROUTE_PLAN, "ignored"),
        )
    }

    @Test
    fun decisionsCarryTheCallerSuppliedClock() {
        val sink = RecordingSink()
        AlasRecognitionDecisionLog.request(sink, at + 42, null, false, false)
        assertEquals(at + 42, sink.decisions.single().atMillis)
    }

    @Test
    fun holderStartsDiscardingAndCanBeInstalled() {
        try {
            // An unwired host must not crash: the default sink swallows decisions.
            AlasDecisionSinkHolder.reset()
            AlasRecognitionDecisionLog.request(AlasDecisionSinkHolder.sink, at, null, false, false)

            val sink = RecordingSink()
            AlasDecisionSinkHolder.sink = sink
            AlasRecognitionDecisionLog.request(AlasDecisionSinkHolder.sink, at, "{}", false, false)
            assertEquals(1, sink.decisions.size)
        } finally {
            AlasDecisionSinkHolder.reset()
        }
    }
}

package com.justccerly.alas.maafw

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * One Alas-side decision, recorded as it happens.
 *
 * The host already persists MaaFramework events (`Controller.Action.*`, task start/finish)
 * and MaaFramework's own `maafw.log`, but nothing in that stream shows what the Alas layer
 * decided: what parameters the custom recognition callback received, which map it decoded,
 * which box it wrote back, or how a route was planned. Those decisions are the ones that
 * need to be visible when a real-device run goes wrong, so they are modelled here as a
 * separate, append-only record rather than being folded into the host's framework events.
 */
@Serializable
data class AlasDecision(
    /** Caller-supplied clock, so tests never depend on wall time. */
    val atMillis: Long,
    val kind: AlasDecisionKind,
    /** Single-line human summary; newlines are folded so one decision stays one log line. */
    val summary: String,
    /** Optional JSON payload, kept as an opaque string so this module owns no wire format. */
    val detail: String? = null,
) {
    init {
        require(summary.isNotBlank()) { "decision summary must not be blank" }
    }
}

/** Stable identifiers, so a log consumer can filter without parsing [AlasDecision.summary]. */
@Serializable
enum class AlasDecisionKind {
    /** A custom recognition callback was entered and its parameters decoded. */
    RECOGNITION_REQUEST,

    /** The callback decoded the replay map and produced an out_box/out_detail answer. */
    RECOGNITION_RESULT,

    /** The callback rejected its input and returned failure to MaaFramework. */
    RECOGNITION_FAILURE,

    /** A route was planned from a recognition document. */
    ROUTE_PLAN,

    /** An action plan was encoded into a Pipeline override. */
    ACTION_PLAN,
}

/**
 * Append-only sink for [AlasDecision].
 *
 * The host maps this onto whatever it already writes; this module never touches a file so it
 * stays usable from a unit test and from the privileged process alike.
 */
fun interface AlasDecisionSink {
    fun record(decision: AlasDecision)

    companion object {
        /** For callers that have no logging yet, and for tests that do not care. */
        val Discarding = AlasDecisionSink { }
    }
}

/**
 * Process-level sink the host installs once at startup.
 *
 * The recognition callback is registered as a JNA function pointer, so the host cannot pass
 * a per-run sink through the C ABI; a process-wide holder is the only place it can be reached
 * from. Defaults to discarding so an unwired host still runs, just without Alas-side logging.
 */
object AlasDecisionSinkHolder {
    @Volatile
    var sink: AlasDecisionSink = AlasDecisionSink.Discarding

    /** Restores the default; used by tests and by a host tearing logging down. */
    fun reset() {
        sink = AlasDecisionSink.Discarding
    }
}

/**
 * Builds the decisions for one recognition callback invocation.
 *
 * Kept separate from the callback itself so the formatting can be tested without JNA,
 * native handles or an Android device.
 */
object AlasRecognitionDecisionLog {
    private val json = Json { explicitNulls = false }
    private const val MAX_SUMMARY = 200

    /** Records the inbound parameters before any decoding is attempted. */
    fun request(
        sink: AlasDecisionSink,
        atMillis: Long,
        paramJson: String?,
        roiPresent: Boolean,
        imagePresent: Boolean,
    ) {
        val length = paramJson?.length ?: 0
        sink.record(
            AlasDecision(
                atMillis = atMillis,
                kind = AlasDecisionKind.RECOGNITION_REQUEST,
                summary = "recognition request: paramBytes=$length roi=$roiPresent image=$imagePresent",
                detail = paramJson?.takeIf { it.isNotBlank() },
            ),
        )
    }

    /** Records the decoded map and the box handed back to MaaFramework. */
    fun result(
        sink: AlasDecisionSink,
        atMillis: Long,
        request: MaaMapRecognitionCallbackCodec.Request,
        box: MaaRecognitionBox,
        filteredCells: Int? = null,
    ) {
        val map = request.map
        val observed = map.cells.size
        val filterNote = filteredCells?.let { " kept=$it" } ?: ""
        sink.record(
            AlasDecision(
                atMillis = atMillis,
                kind = AlasDecisionKind.RECOGNITION_RESULT,
                summary = truncate(
                    "recognition result: map=${map.width}x${map.height} cells=$observed$filterNote " +
                        "box=${box.x},${box.y},${box.width},${box.height}",
                ),
                detail = json.encodeToString(
                    ResultDetail(
                        width = map.width,
                        height = map.height,
                        cells = observed,
                        box = BoxDetail(box.x, box.y, box.width, box.height),
                    ),
                ),
            ),
        )
    }

    /**
     * Records a rejected invocation. The failure reason is the whole point of this entry, so
     * it is never dropped even when long.
     */
    fun failure(sink: AlasDecisionSink, atMillis: Long, error: Throwable) {
        val message = error.message.orEmpty()
        sink.record(
            AlasDecision(
                atMillis = atMillis,
                kind = AlasDecisionKind.RECOGNITION_FAILURE,
                summary = truncate(
                    "recognition failed: ${error.javaClass.simpleName}: $message",
                ),
            ),
        )
    }

    /** Records a planned route, including the unreachable case which produces no taps. */
    fun route(sink: AlasDecisionSink, atMillis: Long, reportJson: String) {
        sink.record(
            AlasDecision(
                atMillis = atMillis,
                kind = AlasDecisionKind.ROUTE_PLAN,
                summary = "route planned",
                detail = reportJson,
            ),
        )
    }

    private fun truncate(text: String): String {
        val folded = text.replace('\n', ' ').replace('\r', ' ')
        return if (folded.length <= MAX_SUMMARY) folded else folded.take(MAX_SUMMARY) + "..."
    }

    @Serializable
    private data class ResultDetail(
        val width: Int,
        val height: Int,
        val cells: Int,
        val box: BoxDetail,
    )

    @Serializable
    private data class BoxDetail(val x: Int, val y: Int, val width: Int, val height: Int)
}

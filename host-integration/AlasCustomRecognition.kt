package com.aliothmoon.maafw.remote

import com.aliothmoon.maafw.maa.MaaFrameworkLibrary
import com.aliothmoon.maafw.maa.MaaFrameworkLoader
import com.aliothmoon.maafw.third.Ln
import com.justccerly.alas.maafw.AlasDecisionSinkHolder
import com.justccerly.alas.maafw.AlasRecognitionDecisionLog
import com.justccerly.alas.maafw.MaaMapRecognitionCallbackCodec
import com.justccerly.alas.maafw.MaaRecognitionBox
import com.sun.jna.Pointer

/** Replayable v5.10.5 Custom Recognition bridge for map recognition. */
internal object AlasCustomRecognition {
    const val NAME = "AlasMapRecognition"

    val callback = MaaFrameworkLibrary.MaaCustomRecognitionCallback {
            _, _, _, _, parameter, _, roi, _, outBox, outDetail,
        ->
        val startedAt = System.currentTimeMillis()
        runCatching {
            val library = MaaFrameworkLoader.library ?: return@runCatching 0.toByte()
            // Recorded before decoding: when the payload itself is the problem, the raw
            // parameters are the only way to tell malformed input from a decode bug.
            AlasRecognitionDecisionLog.request(
                sink = AlasDecisionSinkHolder.sink,
                atMillis = startedAt,
                paramJson = parameter,
                roiPresent = roi != null,
                imagePresent = false,
            )
            val request = MaaMapRecognitionCallbackCodec.decodeRequest(parameter.orEmpty())
            val box = request.box ?: MaaRecognitionBox(0, 0, request.map.width, request.map.height)
            require(outBox != null) { "Maa custom recognition out_box is null" }
            outBox.setInt(0, box.x)
            outBox.setInt(4, box.y)
            outBox.setInt(8, box.width)
            outBox.setInt(12, box.height)
            val detail = MaaMapRecognitionCallbackCodec.encodeDetail(request.map)
            check(library.MaaStringBufferSet(outDetail, detail).toInt() != 0) {
                "MaaStringBufferSet rejected map recognition detail"
            }
            AlasRecognitionDecisionLog.result(
                sink = AlasDecisionSinkHolder.sink,
                atMillis = System.currentTimeMillis(),
                request = request,
                box = box,
            )
            1.toByte()
        }.getOrElse { error ->
            AlasRecognitionDecisionLog.failure(
                sink = AlasDecisionSinkHolder.sink,
                atMillis = System.currentTimeMillis(),
                error = error,
            )
            Ln.w("AlasCustomRecognition failed: ${error.message}")
            0.toByte()
        }
    }
}

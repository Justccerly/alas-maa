package com.justccerly.alas.maafw

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/** Screen-space rectangle returned through MaaFramework's `MaaRect* out_box`. */
@Serializable
data class MaaRecognitionBox(
    val x: Int,
    val y: Int,
    val width: Int,
    val height: Int,
) {
    init {
        require(width > 0 && height > 0) { "recognition box dimensions must be positive" }
    }
}

/**
 * Decoded result of MaaFramework v5.9.2 `MaaCustomRecognitionCallback`.
 * `out_detail` carries the versioned map document while `out_box` carries the
 * screen-space map bounds. Keeping those outputs separate mirrors the C ABI.
 */
data class MaaMapRecognitionResult(
    val box: MaaRecognitionBox,
    val map: MapRecognitionDocument,
)

/** Stable JSON protocol for the custom recognition callback's `out_detail`. */
object MaaMapRecognitionCallbackCodec {
    private const val SCHEMA = "alas.map-recognition.v1"
    private val json = Json { ignoreUnknownKeys = true; explicitNulls = false }

    @Serializable
    private data class Detail(
        val schema: String,
        val map: MapRecognitionDocument,
    )

    fun encodeDetail(map: MapRecognitionDocument): String =
        json.encodeToString(Detail(schema = SCHEMA, map = map))

    fun decode(box: MaaRecognitionBox, detailJson: String): MaaMapRecognitionResult {
        val detail = json.decodeFromString<Detail>(detailJson)
        require(detail.schema == SCHEMA) {
            "unsupported Maa custom recognition detail schema: ${detail.schema}"
        }
        return MaaMapRecognitionResult(box = box, map = detail.map)
    }

    fun toRecognitionJson(result: MaaMapRecognitionResult): String =
        MapRecognitionJson.encodeDocument(result.map)
}

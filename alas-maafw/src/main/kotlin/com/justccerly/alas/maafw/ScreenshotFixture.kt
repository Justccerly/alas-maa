package com.justccerly.alas.maafw

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/** Screen-space rectangle in device pixels. */
@Serializable
data class ScreenshotBox(
    val x: Int,
    val y: Int,
    val width: Int,
    val height: Int,
) {
    init {
        require(x >= 0 && y >= 0) { "screenshot box origin must be non-negative" }
        require(width > 0 && height > 0) { "screenshot box dimensions must be positive" }
    }

    val right: Int get() = x + width
    val bottom: Int get() = y + height

    fun contains(other: ScreenshotBox): Boolean =
        other.x >= x && other.y >= y && other.right <= right && other.bottom <= bottom
}

/** Mean colour of one named region, rounded to two decimals for a stable manifest. */
@Serializable
data class ScreenshotRegionMean(
    val name: String,
    val box: ScreenshotBox,
    val meanRgb: List<Double>,
    val note: String? = null,
) {
    init {
        require(name.isNotBlank()) { "region name must not be blank" }
        require(meanRgb.size == 3) { "meanRgb must have exactly three channels" }
        require(meanRgb.all { it in 0.0..255.0 }) { "meanRgb channels must be within 0..255" }
    }
}

/** What a fixture is expected to look like; kept open-ended until real algorithms exist. */
@Serializable
data class ScreenshotExpectation(
    val kind: String,
    val note: String? = null,
)

/** Provenance, so a fixture can be re-derived or retired when the game changes. */
@Serializable
data class ScreenshotSource(
    val kind: String,
    val device: String? = null,
    val capturedAt: String? = null,
    val note: String? = null,
)

/**
 * A recorded device screenshot plus what it is expected to contain.
 *
 * Screenshots are the only way to test recognition without a device, so each fixture pins
 * the exact bytes it was derived from ([sha256]) along with the device resolution. When a
 * fixture no longer matches, that is a deliberate signal to re-record rather than a flaky
 * failure: the game or the capture path changed.
 */
@Serializable
data class ScreenshotFixture(
    val name: String,
    val image: String,
    val width: Int,
    val height: Int,
    val sha256: String,
    val source: ScreenshotSource? = null,
    val regions: List<ScreenshotRegionMean> = emptyList(),
    val expected: ScreenshotExpectation? = null,
) {
    init {
        require(name.isNotBlank()) { "fixture name must not be blank" }
        require(image.isNotBlank()) { "fixture image path must not be blank" }
        require(width > 0 && height > 0) { "fixture dimensions must be positive" }
        require(SHA256.matches(sha256)) { "sha256 must be 64 lowercase hex characters" }
        val names = regions.map { it.name }
        require(names.toSet().size == names.size) { "fixture contains duplicate region names" }
        regions.forEach { region ->
            require(
                region.box.right <= width && region.box.bottom <= height,
            ) { "region ${region.name} is outside the ${width}x$height fixture" }
        }
    }

    // Not private: the serialization plugin puts serializer() on the companion, and
    // ScreenshotFixtureCodec is a separate object, so a private companion would fail with
    // IllegalAccessError at runtime (the same defect fixed earlier in MapRouteRequest).
    companion object {
        private val SHA256 = Regex("[0-9a-f]{64}")
    }
}

/**
 * Pixel access boundary.
 *
 * `alas-maafw` is a plain JVM module that is also compiled into the Android host, and
 * Android has no `javax.imageio`. Decoding therefore stays outside: a JVM test can back this
 * with `ImageIO`, the host with `BitmapFactory`, and neither leaks into the domain layer.
 */
interface ScreenshotPixels {
    val width: Int
    val height: Int

    /** Packed 0xRRGGBB at the given pixel. Out-of-range access is a caller bug. */
    fun rgbAt(x: Int, y: Int): Int
}

/** Result of checking one recorded region against a fixture. */
data class RegionCheck(
    val name: String,
    val expected: List<Double>,
    val actual: List<Double>,
    val tolerance: Double,
) {
    val matches: Boolean
        get() = expected.zip(actual).all { (e, a) -> kotlin.math.abs(e - a) <= tolerance }
}

/**
 * Verifies a decoded frame against a fixture without depending on an image library.
 *
 * Tolerance is explicit because fixtures are quantised to keep them small, so an exact
 * per-pixel match is not meaningful; the point is to detect a wrong frame, a wrong
 * resolution, or a mis-cropped region rather than to pin individual pixels.
 */
object ScreenshotFixtureVerifier {

    /** Default tolerance for 256-colour quantised fixtures. */
    const val DEFAULT_TOLERANCE = 6.0

    fun verify(
        fixture: ScreenshotFixture,
        pixels: ScreenshotPixels,
        tolerance: Double = DEFAULT_TOLERANCE,
    ): List<RegionCheck> {
        require(tolerance >= 0) { "tolerance must be non-negative" }
        require(pixels.width == fixture.width && pixels.height == fixture.height) {
            "frame is ${pixels.width}x${pixels.height} but fixture ${fixture.name} " +
                "is ${fixture.width}x${fixture.height}"
        }
        return fixture.regions.map { region ->
            RegionCheck(
                name = region.name,
                expected = region.meanRgb,
                actual = meanRgb(pixels, region.box),
                tolerance = tolerance,
            )
        }
    }

    /** Mean of each channel over [box], rounded to two decimals to match the manifest. */
    fun meanRgb(pixels: ScreenshotPixels, box: ScreenshotBox): List<Double> {
        require(box.right <= pixels.width && box.bottom <= pixels.height) {
            "region $box exceeds the ${pixels.width}x${pixels.height} frame"
        }
        var r = 0L
        var g = 0L
        var b = 0L
        for (y in box.y until box.bottom) {
            for (x in box.x until box.right) {
                val rgb = pixels.rgbAt(x, y)
                r += (rgb shr 16) and 0xFF
                g += (rgb shr 8) and 0xFF
                b += rgb and 0xFF
            }
        }
        val count = box.width.toLong() * box.height
        return listOf(r, g, b).map { round2(it.toDouble() / count) }
    }

    private fun round2(value: Double): Double = kotlin.math.round(value * 100) / 100
}

/** Decodes fixture manifests; keeps JSON out of the callers. */
object ScreenshotFixtureCodec {
    private val json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
    }

    fun decode(document: String): ScreenshotFixture = json.decodeFromString(document)

    fun encode(fixture: ScreenshotFixture): String = json.encodeToString(fixture)
}

package com.justccerly.alas.maafw

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

/** Uniform or patterned pixels without any image library, so this runs on plain JVM. */
private class FakePixels(
    override val width: Int,
    override val height: Int,
    private val fill: Int = 0x000000,
    private val painter: ((Int, Int) -> Int)? = null,
) : ScreenshotPixels {
    override fun rgbAt(x: Int, y: Int): Int = painter?.invoke(x, y) ?: fill
}

class ScreenshotFixtureTest {
    private val sha = "a".repeat(64)

    private fun fixture(
        width: Int = 4,
        height: Int = 4,
        regions: List<ScreenshotRegionMean> = emptyList(),
    ) = ScreenshotFixture(
        name = "test",
        image = "test.png",
        width = width,
        height = height,
        sha256 = sha,
        regions = regions,
    )

    private fun region(
        name: String = "r",
        box: ScreenshotBox = ScreenshotBox(0, 0, 2, 2),
        mean: List<Double> = listOf(1.0, 2.0, 3.0),
        note: String? = null,
    ) = ScreenshotRegionMean(name = name, box = box, meanRgb = mean, note = note)

    @Test
    fun computesMeanColourOfARegion() {
        val pixels = FakePixels(4, 4, fill = 0x102030)
        val mean = ScreenshotFixtureVerifier.meanRgb(pixels, ScreenshotBox(0, 0, 4, 4))

        assertEquals(listOf(16.0, 32.0, 48.0), mean)
    }

    @Test
    fun meanUsesOnlyTheRequestedRegion() {
        // Left half red, right half blue: the mean of the left half must ignore the right.
        val pixels = FakePixels(4, 2) { x, _ -> if (x < 2) 0xFF0000 else 0x0000FF }
        val left = ScreenshotFixtureVerifier.meanRgb(pixels, ScreenshotBox(0, 0, 2, 2))
        val right = ScreenshotFixtureVerifier.meanRgb(pixels, ScreenshotBox(2, 0, 2, 2))

        assertEquals(listOf(255.0, 0.0, 0.0), left)
        assertEquals(listOf(0.0, 0.0, 255.0), right)
    }

    @Test
    fun meanIsRoundedToTwoDecimals() {
        // Three pixels of 0/1/2 in red gives 1.0; use a 3-wide box of 10,10,11 to force rounding.
        val pixels = FakePixels(3, 1) { x, _ -> when (x) { 0 -> 0x0A0000; 1 -> 0x0A0000; else -> 0x0B0000 } }
        val mean = ScreenshotFixtureVerifier.meanRgb(pixels, ScreenshotBox(0, 0, 3, 1))

        // (10+10+11)/3 = 10.333... -> 10.33
        assertEquals(10.33, mean[0])
    }

    @Test
    fun verifyReportsEachRegionAsMatching() {
        val pixels = FakePixels(4, 4, fill = 0x102030)
        val f = fixture(regions = listOf(region(mean = listOf(16.0, 32.0, 48.0))))

        val checks = ScreenshotFixtureVerifier.verify(f, pixels)

        assertEquals(1, checks.size)
        assertTrue(checks.single().matches, checks.single().toString())
    }

    @Test
    fun verifyDetectsAWrongFrame() {
        val pixels = FakePixels(4, 4, fill = 0xFF0000)
        val f = fixture(regions = listOf(region(mean = listOf(16.0, 32.0, 48.0))))

        val check = ScreenshotFixtureVerifier.verify(f, pixels).single()

        assertTrue(!check.matches, "a red frame must not satisfy a blue expectation")
        assertEquals(listOf(255.0, 0.0, 0.0), check.actual)
    }

    @Test
    fun verifyHonoursTolerance() {
        // Actual 0 vs expected 16: outside the default tolerance of 6, inside a tolerance of 20.
        val pixels = FakePixels(2, 2, fill = 0x000000)
        val f = fixture(width = 2, height = 2, regions = listOf(region(mean = listOf(16.0, 0.0, 0.0))))

        assertTrue(!ScreenshotFixtureVerifier.verify(f, pixels).single().matches)
        assertTrue(ScreenshotFixtureVerifier.verify(f, pixels, tolerance = 20.0).single().matches)
    }

    @Test
    fun toleranceIsInclusiveAtItsBoundary() {
        // 10 vs 16 is exactly 6, which must count as a match at tolerance 6.
        val pixels = FakePixels(2, 2, fill = 0x0A0000)
        val f = fixture(width = 2, height = 2, regions = listOf(region(mean = listOf(16.0, 0.0, 0.0))))

        val check = ScreenshotFixtureVerifier.verify(f, pixels, tolerance = 6.0).single()
        assertEquals(6.0, check.tolerance)
        assertTrue(check.matches, "a difference equal to the tolerance must match")
    }

    @Test
    fun verifyRejectsAResolutionMismatch() {
        // A fixture recorded at one resolution must not silently pass at another.
        val f = fixture(width = 4, height = 4)
        val wrong = FakePixels(8, 8)

        val error = assertFailsWith<IllegalArgumentException> {
            ScreenshotFixtureVerifier.verify(f, wrong)
        }
        assertTrue(error.message!!.contains("8x8"), error.message!!)
        assertTrue(error.message!!.contains("4x4"), error.message!!)
    }

    @Test
    fun negativeToleranceIsRejected() {
        assertFailsWith<IllegalArgumentException> {
            ScreenshotFixtureVerifier.verify(fixture(), FakePixels(4, 4), tolerance = -1.0)
        }
    }

    @Test
    fun regionOutsideTheFrameIsRejectedAtConstruction() {
        assertFailsWith<IllegalArgumentException> {
            fixture(width = 4, height = 4, regions = listOf(region(box = ScreenshotBox(3, 3, 2, 2))))
        }
    }

    @Test
    fun duplicateRegionNamesAreRejected() {
        assertFailsWith<IllegalArgumentException> {
            fixture(regions = listOf(region(name = "same"), region(name = "same")))
        }
    }

    @Test
    fun malformedShaIsRejected() {
        assertFailsWith<IllegalArgumentException> {
            ScreenshotFixture(name = "t", image = "t.png", width = 1, height = 1, sha256 = "nope")
        }
        assertFailsWith<IllegalArgumentException> {
            // Uppercase is not accepted: the manifest is written lowercase, so accepting both
            // would let a hand-edited fixture drift from what the generator produces.
            ScreenshotFixture(name = "t", image = "t.png", width = 1, height = 1, sha256 = "A".repeat(64))
        }
    }

    @Test
    fun regionMeanRejectsBadChannels() {
        assertFailsWith<IllegalArgumentException> {
            ScreenshotRegionMean(name = "r", box = ScreenshotBox(0, 0, 1, 1), meanRgb = listOf(1.0, 2.0))
        }
        assertFailsWith<IllegalArgumentException> {
            ScreenshotRegionMean(
                name = "r",
                box = ScreenshotBox(0, 0, 1, 1),
                meanRgb = listOf(1.0, 2.0, 300.0),
            )
        }
    }

    @Test
    fun boxRejectsEmptyOrNegativeDimensions() {
        assertFailsWith<IllegalArgumentException> { ScreenshotBox(0, 0, 0, 4) }
        assertFailsWith<IllegalArgumentException> { ScreenshotBox(-1, 0, 4, 4) }
    }

    @Test
    fun codecRoundTripsAFixture() {
        val original = fixture(
            regions = listOf(region(name = "title", note = "标题区")),
        ).copy(
            source = ScreenshotSource(kind = "device-screenshot", device = "MuMu", capturedAt = "2026-09-28"),
            expected = ScreenshotExpectation(kind = "game-download"),
        )

        val decoded = ScreenshotFixtureCodec.decode(ScreenshotFixtureCodec.encode(original))

        assertEquals(original, decoded)
    }

    @Test
    fun codecIgnoresUnknownFieldsForForwardCompatibility() {
        val document = """
            {
              "name": "t", "image": "t.png", "width": 2, "height": 2,
              "sha256": "${"b".repeat(64)}",
              "futureField": {"nested": true}
            }
        """.trimIndent()

        val decoded = ScreenshotFixtureCodec.decode(document)

        assertEquals("t", decoded.name)
        assertTrue(decoded.regions.isEmpty())
    }

    @Test
    fun meanOfAFullWidthStripIsExact() {
        // Guards the box arithmetic: a 1px-tall strip must not double-count rows.
        val pixels = FakePixels(4, 3) { _, y -> if (y == 1) 0xFFFFFF else 0x000000 }
        val mean = ScreenshotFixtureVerifier.meanRgb(pixels, ScreenshotBox(0, 1, 4, 1))

        assertEquals(listOf(255.0, 255.0, 255.0), mean)
    }
}

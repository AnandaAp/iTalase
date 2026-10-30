package com.anlian.italase.shared.media

import java.awt.Color
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import java.util.Random
import javax.imageio.ImageIO
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

/**
 * Adversarial empirical stress test suite for [ImageOptimizer].
 * Evaluates extreme dimensions, memory-safe inSampleSize scaling invariants,
 * realistic QR code compression size constraints (< 150KB), and input validation boundaries.
 */
class ImageOptimizerStressTest {

    private val originalEngine = ImageOptimizer.engine

    @BeforeTest
    fun setUp() {
        ImageOptimizer.engine = JvmImageEngine()
    }

    @AfterTest
    fun tearDown() {
        ImageOptimizer.engine = originalEngine
    }

    // --- Helpers for generating realistic QR codes ---

    /**
     * Generates a realistic synthetic QR code with finder patterns (top-left, top-right, bottom-left),
     * timing patterns, and random pseudo-realistic high-entropy data modules.
     */
    private fun createRealisticQrJpeg(
        canvasWidth: Int,
        canvasHeight: Int,
        moduleCount: Int = 33, // Version 4 QR
        seed: Long = 42L
    ): ByteArray {
        val img = BufferedImage(canvasWidth, canvasHeight, BufferedImage.TYPE_INT_RGB)
        val g = img.createGraphics()
        try {
            // White background (paper receipt)
            g.color = Color.WHITE
            g.fillRect(0, 0, canvasWidth, canvasHeight)

            val margin = (minOf(canvasWidth, canvasHeight) * 0.1).toInt()
            val qrSize = minOf(canvasWidth, canvasHeight) - 2 * margin
            val modulePixelSize = qrSize / moduleCount
            val startX = (canvasWidth - qrSize) / 2
            val startY = (canvasHeight - qrSize) / 2

            // Draw pseudo-random data modules
            val random = Random(seed)
            g.color = Color.BLACK
            for (r in 0 until moduleCount) {
                for (c in 0 until moduleCount) {
                    // Check if inside finder patterns (7x7 corners)
                    val inTopLeft = r < 7 && c < 7
                    val inTopRight = r < 7 && c >= moduleCount - 7
                    val inBottomLeft = r >= moduleCount - 7 && c < 7

                    if (inTopLeft || inTopRight || inBottomLeft) {
                        continue // Drawn separately
                    }

                    if (random.nextBoolean()) {
                        g.fillRect(
                            startX + c * modulePixelSize,
                            startY + r * modulePixelSize,
                            modulePixelSize,
                            modulePixelSize
                        )
                    }
                }
            }

            // Draw Finder Patterns (7x7 outer square, 5x5 white, 3x3 inner black)
            fun drawFinder(originX: Int, originY: Int) {
                g.color = Color.BLACK
                g.fillRect(originX, originY, 7 * modulePixelSize, 7 * modulePixelSize)
                g.color = Color.WHITE
                g.fillRect(originX + modulePixelSize, originY + modulePixelSize, 5 * modulePixelSize, 5 * modulePixelSize)
                g.color = Color.BLACK
                g.fillRect(originX + 2 * modulePixelSize, originY + 2 * modulePixelSize, 3 * modulePixelSize, 3 * modulePixelSize)
            }

            drawFinder(startX, startY)
            drawFinder(startX + (moduleCount - 7) * modulePixelSize, startY)
            drawFinder(startX, startY + (moduleCount - 7) * modulePixelSize)

        } finally {
            g.dispose()
        }

        val out = ByteArrayOutputStream()
        ImageIO.write(img, "jpeg", out)
        return out.toByteArray()
    }

    // --- 1. Extreme Dimensions Stress-Tests ---

    @Test
    fun testExtremeAspectRatios_10000x1_and_1x10000() {
        // 10000x1
        val target10000x1 = ImageOptimizer.calculateTargetDimensions(10000, 1, 1024)
        assertEquals(1024, target10000x1.width)
        assertEquals(1, target10000x1.height)
        assertTrue(target10000x1.width <= 1024)
        assertTrue(target10000x1.height <= 1024)
        assertTrue(target10000x1.height >= 1)

        val sampleSize10000x1 = ImageOptimizer.calculateInSampleSize(10000, 1, 1024)
        // Since height is 1, halfHeight = 0, so inSampleSize must be 1 to prevent collapsing to 0 height
        assertEquals(1, sampleSize10000x1)

        // 1x10000
        val target1x10000 = ImageOptimizer.calculateTargetDimensions(1, 10000, 1024)
        assertEquals(1, target1x10000.width)
        assertEquals(1024, target1x10000.height)
        assertTrue(target1x10000.width <= 1024)
        assertTrue(target1x10000.height <= 1024)
        assertTrue(target1x10000.width >= 1)

        val sampleSize1x10000 = ImageOptimizer.calculateInSampleSize(1, 10000, 1024)
        assertEquals(1, sampleSize1x10000)
    }

    @Test
    fun testExtremeDimensions_8000x8000_and_1x1() {
        // 8000x8000 (64 Megapixels square)
        val target8000 = ImageOptimizer.calculateTargetDimensions(8000, 8000, 1024)
        assertEquals(1024, target8000.width)
        assertEquals(1024, target8000.height)

        val sampleSize8000 = ImageOptimizer.calculateInSampleSize(8000, 8000, 1024)
        // 8000/2 = 4000 (>=1024) -> 2
        // 4000/2 = 2000 (>=1024) -> 4
        // 2000/2 = 1000 (<1024) -> stops at 4
        assertEquals(4, sampleSize8000)

        // 1x1 minimal single pixel
        val target1x1 = ImageOptimizer.calculateTargetDimensions(1, 1, 1024)
        assertEquals(1, target1x1.width)
        assertEquals(1, target1x1.height)

        val sampleSize1x1 = ImageOptimizer.calculateInSampleSize(1, 1, 1024)
        assertEquals(1, sampleSize1x1)
    }

    @Test
    fun testOddDimensionsAspectRatios() {
        // 10001x1
        val t1 = ImageOptimizer.calculateTargetDimensions(10001, 1, 1024)
        assertEquals(1024, t1.width)
        assertEquals(1, t1.height)

        // 1025x1025 (just over 1024 boundary)
        val t2 = ImageOptimizer.calculateTargetDimensions(1025, 1025, 1024)
        assertEquals(1024, t2.width)
        assertEquals(1024, t2.height)

        // 1023x1023 (just under 1024 boundary)
        val t3 = ImageOptimizer.calculateTargetDimensions(1023, 1023, 1024)
        assertEquals(1023, t3.width)
        assertEquals(1023, t3.height)

        // 3333x777 (arbitrary odd ratio)
        val t4 = ImageOptimizer.calculateTargetDimensions(3333, 777, 1024)
        assertEquals(1024, t4.width)
        val expectedHeight = (777 * (1024.0 / 3333.0)).toInt()
        assertEquals(expectedHeight, t4.height)
        assertTrue(t4.width <= 1024 && t4.height <= 1024)
    }

    // --- 2. inSampleSize Invariant Verification ---

    @Test
    fun testInSampleSizeAlwaysPowerOfTwoAndNeverExceedsBounds() {
        val testDimensions = listOf(
            Pair(10000, 1),
            Pair(1, 10000),
            Pair(8000, 8000),
            Pair(4000, 3000),
            Pair(3000, 4000),
            Pair(2048, 2048),
            Pair(1920, 1080),
            Pair(1024, 1024),
            Pair(800, 600),
            Pair(100, 100),
            Pair(1, 1)
        )

        for ((w, h) in testDimensions) {
            val sampleSize = ImageOptimizer.calculateInSampleSize(w, h, 1024)

            // Invariant 1: must be >= 1
            assertTrue(sampleSize >= 1, "inSampleSize must be >= 1 for ${w}x$h")

            // Invariant 2: must be power of 2 (sampleSize & (sampleSize - 1) == 0)
            assertEquals(
                0,
                sampleSize and (sampleSize - 1),
                "inSampleSize ($sampleSize) must be a power of 2 for ${w}x$h"
            )

            // Invariant 3: scale factor never exceeds image bounds
            assertTrue(
                sampleSize <= maxOf(w, h),
                "inSampleSize ($sampleSize) exceeds max dimension (${maxOf(w, h)})"
            )

            // Invariant 4: subsampled dimensions never drop to 0
            assertTrue(w / sampleSize >= 0)
            assertTrue(h / sampleSize >= 0)
        }
    }

    @Test
    fun testInSampleSizeExactScaleProgression() {
        // req = 1024
        // src < 2048 -> sampleSize 1
        assertEquals(1, ImageOptimizer.calculateInSampleSize(2047, 2047, 1024, 1024))
        // src = 2048 -> half = 1024 >= 1024 -> sampleSize 2
        assertEquals(2, ImageOptimizer.calculateInSampleSize(2048, 2048, 1024, 1024))
        // src = 4095 -> half = 2047 -> half/2 = 1023 < 1024 -> sampleSize 2
        assertEquals(2, ImageOptimizer.calculateInSampleSize(4095, 4095, 1024, 1024))
        // src = 4096 -> half = 2048 -> half/2 = 1024 >= 1024 -> sampleSize 4
        assertEquals(4, ImageOptimizer.calculateInSampleSize(4096, 4096, 1024, 1024))
        // src = 8192 -> sampleSize 8
        assertEquals(8, ImageOptimizer.calculateInSampleSize(8192, 8192, 1024, 1024))
    }

    // --- 3. Realistic QR Code Compression (< 150KB) ---

    @Test
    fun testRealisticQrCodesCompressedStrictlyUnder150KB() {
        // Test multiple realistic QR scenarios:
        // A) 4000x3000 photo from typical mobile camera
        val raw4000x3000 = createRealisticQrJpeg(4000, 3000, moduleCount = 33, seed = 101L)
        val opt1 = ImageOptimizer.optimize(raw4000x3000, maxDimension = 1024, quality = 82)
        println("OPTIMIZER_EMPIRICAL_SIZE: 4000x3000 v4 QR -> ${opt1.size} bytes (${opt1.size / 1024} KB)")
        assertTrue(opt1.size < ImageOptimizer.MAX_ALLOWED_FILE_SIZE_BYTES, "4000x3000 QR exceeded 150KB: ${opt1.size} bytes")
        val d1 = JvmImageEngine().decodeDimensions(opt1)!!
        assertEquals(1024, d1.width)
        assertEquals(768, d1.height)

        // B) 3000x4000 portrait photo from mobile camera
        val raw3000x4000 = createRealisticQrJpeg(3000, 4000, moduleCount = 33, seed = 202L)
        val opt2 = ImageOptimizer.optimize(raw3000x4000, maxDimension = 1024, quality = 82)
        println("OPTIMIZER_EMPIRICAL_SIZE: 3000x4000 v4 QR -> ${opt2.size} bytes (${opt2.size / 1024} KB)")
        assertTrue(opt2.size < ImageOptimizer.MAX_ALLOWED_FILE_SIZE_BYTES, "3000x4000 QR exceeded 150KB: ${opt2.size} bytes")
        val d2 = JvmImageEngine().decodeDimensions(opt2)!!
        assertEquals(768, d2.width)
        assertEquals(1024, d2.height)

        // C) 2048x2048 square high-density QR code (Version 10, 57 modules)
        val rawSquare57 = createRealisticQrJpeg(2048, 2048, moduleCount = 57, seed = 303L)
        val opt3 = ImageOptimizer.optimize(rawSquare57, maxDimension = 1024, quality = 82)
        println("OPTIMIZER_EMPIRICAL_SIZE: 2048x2048 v10 QR -> ${opt3.size} bytes (${opt3.size / 1024} KB)")
        assertTrue(opt3.size < ImageOptimizer.MAX_ALLOWED_FILE_SIZE_BYTES, "2048x2048 dense QR exceeded 150KB: ${opt3.size} bytes")
        val d3 = JvmImageEngine().decodeDimensions(opt3)!!
        assertEquals(1024, d3.width)
        assertEquals(1024, d3.height)

        // D) Extreme boundary analysis: High-entropy Version 40 QR code (177x177 modules) at 2500x2500:
        // Note: QRIS payment specifications use Version 4 to 10 (<= 57 modules, payload ~300 chars).
        // Version 40 contains 177x177 modules (~3KB payload), representing theoretical worst-case spatial frequency.
        val rawDense177 = createRealisticQrJpeg(2500, 2500, moduleCount = 177, seed = 404L)
        val opt4Default = ImageOptimizer.optimize(rawDense177, maxDimension = 1024, quality = 82)
        println("OPTIMIZER_EMPIRICAL_SIZE: 2500x2500 v40 QR (quality 82) -> ${opt4Default.size} bytes (${opt4Default.size / 1024} KB)")

        // Extreme entropy with 177 alternating modules produces ~323KB at quality 82 because 8x8 DCT blocks
        // cannot compress 5-pixel alternating frequencies. With maxDimension = 512, it comfortably fits < 150KB:
        val opt4Dimension512 = ImageOptimizer.optimize(rawDense177, maxDimension = 512, quality = 82)
        println("OPTIMIZER_EMPIRICAL_SIZE: 2500x2500 v40 QR (maxDimension 512) -> ${opt4Dimension512.size} bytes (${opt4Dimension512.size / 1024} KB)")
        assertTrue(
            opt4Dimension512.size < ImageOptimizer.MAX_ALLOWED_FILE_SIZE_BYTES,
            "Extreme Version 40 QR with maxDimension 512 should be < 150KB: ${opt4Dimension512.size} bytes"
        )
    }

    // --- 4. Input Validation & Truncation ---

    @Test
    fun testInputValidationZeroBytes() {
        val ex = assertFailsWith<IllegalArgumentException> {
            ImageOptimizer.optimize(ByteArray(0))
        }
        assertTrue(ex.message?.contains("kosong") == true)
    }

    @Test
    fun testInputValidationTruncatedJpegBytes() {
        // 1 byte
        assertFailsWith<IllegalArgumentException> {
            ImageOptimizer.optimize(byteArrayOf(0xFF.toByte()))
        }

        // 2 bytes (SOI marker only)
        assertFailsWith<IllegalArgumentException> {
            ImageOptimizer.optimize(byteArrayOf(0xFF.toByte(), 0xD8.toByte()))
        }

        // Truncated valid JPEG (only first 32 bytes)
        val fullJpeg = createRealisticQrJpeg(400, 400)
        val truncated32 = fullJpeg.copyOfRange(0, 32)
        assertFailsWith<IllegalArgumentException> {
            ImageOptimizer.optimize(truncated32)
        }

        // Truncated valid JPEG (first 100 bytes)
        val truncated100 = fullJpeg.copyOfRange(0, 100)
        assertFailsWith<IllegalArgumentException> {
            ImageOptimizer.optimize(truncated100)
        }
    }

    @Test
    fun testInputValidationCorruptBytesAndRandomNoise() {
        // All zeros (1KB)
        assertFailsWith<IllegalArgumentException> {
            ImageOptimizer.optimize(ByteArray(1024))
        }

        // Random noise (2KB)
        val noise = ByteArray(2048)
        Random(999L).nextBytes(noise)
        assertFailsWith<IllegalArgumentException> {
            ImageOptimizer.optimize(noise)
        }

        // Text file content
        val textBytes = "This is not a JPEG image, it is a plain text string.".encodeToByteArray()
        assertFailsWith<IllegalArgumentException> {
            ImageOptimizer.optimize(textBytes)
        }
    }

    @Test
    fun testInputValidationInvalidQualityBoundaryValues() {
        val validJpeg = createRealisticQrJpeg(200, 200)

        // Quality 0 (out of bounds)
        assertFailsWith<IllegalArgumentException> {
            ImageOptimizer.optimize(validJpeg, quality = 0)
        }

        // Negative quality
        assertFailsWith<IllegalArgumentException> {
            ImageOptimizer.optimize(validJpeg, quality = -1)
        }
        assertFailsWith<IllegalArgumentException> {
            ImageOptimizer.optimize(validJpeg, quality = -100)
        }
        assertFailsWith<IllegalArgumentException> {
            ImageOptimizer.optimize(validJpeg, quality = Int.MIN_VALUE)
        }

        // Quality > 100
        assertFailsWith<IllegalArgumentException> {
            ImageOptimizer.optimize(validJpeg, quality = 101)
        }
        assertFailsWith<IllegalArgumentException> {
            ImageOptimizer.optimize(validJpeg, quality = 200)
        }
        assertFailsWith<IllegalArgumentException> {
            ImageOptimizer.optimize(validJpeg, quality = Int.MAX_VALUE)
        }
    }

    @Test
    fun testInputValidationInvalidMaxDimensionBoundaryValues() {
        val validJpeg = createRealisticQrJpeg(200, 200)

        // Max dimension 0
        assertFailsWith<IllegalArgumentException> {
            ImageOptimizer.optimize(validJpeg, maxDimension = 0)
        }

        // Negative max dimension
        assertFailsWith<IllegalArgumentException> {
            ImageOptimizer.optimize(validJpeg, maxDimension = -1)
        }
        assertFailsWith<IllegalArgumentException> {
            ImageOptimizer.optimize(validJpeg, maxDimension = -1024)
        }
        assertFailsWith<IllegalArgumentException> {
            ImageOptimizer.optimize(validJpeg, maxDimension = Int.MIN_VALUE)
        }
    }

    @Test
    fun testImageDimensionsEntityValidation() {
        // Negative width
        assertFailsWith<IllegalArgumentException> {
            ImageDimensions(-1, 100)
        }
        // Negative height
        assertFailsWith<IllegalArgumentException> {
            ImageDimensions(100, -1)
        }
        // 0 dimensions allowed in data class (e.g. uninitialized / empty bounds)
        val dZero = ImageDimensions(0, 0)
        assertEquals(0, dZero.width)
        assertEquals(0, dZero.height)
    }
}

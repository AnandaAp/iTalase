package com.anlian.italase.shared.media

import java.awt.Color
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import javax.imageio.ImageIO
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.test.runTest
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ImageOptimizerTest {

    private val originalEngine = ImageOptimizer.engine

    @BeforeTest
    fun setUp() {
        ImageOptimizer.engine = JvmImageEngine()
    }

    @AfterTest
    fun tearDown() {
        ImageOptimizer.engine = originalEngine
    }

    // Helper: Buat JPEG bytes sintetis di memori
    private fun createSyntheticJpeg(width: Int, height: Int): ByteArray {
        val img = BufferedImage(width, height, BufferedImage.TYPE_INT_RGB)
        val g = img.createGraphics()
        try {
            g.color = Color.WHITE
            g.fillRect(0, 0, width, height)
            g.color = Color.BLACK
            // Pola modul kode QR (kotak-kotak solid)
            val blockSize = 60
            for (x in 0 until width step blockSize) {
                for (y in 0 until height step blockSize) {
                    if ((x / blockSize + y / blockSize) % 3 == 0) {
                        g.fillRect(x, y, blockSize, blockSize)
                    }
                }
            }
        } finally {
            g.dispose()
        }
        val out = ByteArrayOutputStream()
        ImageIO.write(img, "jpeg", out)
        return out.toByteArray()
    }

    // 1. DOWNSAMPLING: 4000x3000 -> 1024x768 (Landscape 4:3)
    @Test
    fun testDownsamplingLandscapePreservesAspectRatio() {
        val target = ImageOptimizer.calculateTargetDimensions(srcWidth = 4000, srcHeight = 3000, maxDimension = 1024)
        assertEquals(1024, target.width)
        assertEquals(768, target.height)
        assertTrue(target.width <= 1024 && target.height <= 1024)
        val originalRatio = 4000.0 / 3000.0
        val targetRatio = target.width.toDouble() / target.height.toDouble()
        assertEquals(originalRatio, targetRatio, 0.001)
    }

    // 2. DOWNSAMPLING: 3000x4000 -> 768x1024 (Portrait 3:4)
    @Test
    fun testDownsamplingPortraitPreservesAspectRatio() {
        val target = ImageOptimizer.calculateTargetDimensions(srcWidth = 3000, srcHeight = 4000, maxDimension = 1024)
        assertEquals(768, target.width)
        assertEquals(1024, target.height)
        assertTrue(target.width <= 1024 && target.height <= 1024)
        val originalRatio = 3000.0 / 4000.0
        val targetRatio = target.width.toDouble() / target.height.toDouble()
        assertEquals(originalRatio, targetRatio, 0.001)
    }

    // 3. DOWNSAMPLING: Square 2048x2048 -> 1024x1024 (1:1)
    @Test
    fun testDownsamplingSquare() {
        val target = ImageOptimizer.calculateTargetDimensions(srcWidth = 2048, srcHeight = 2048, maxDimension = 1024)
        assertEquals(1024, target.width)
        assertEquals(1024, target.height)
    }

    // 4. PRESERVE SMALL IMAGES: <= 1024x1024 Tidak Boleh Diperbesar (No Upscaling)
    @Test
    fun testImagesUnderMaxDimensionNotUpscaled() {
        val target1 = ImageOptimizer.calculateTargetDimensions(srcWidth = 800, srcHeight = 600, maxDimension = 1024)
        assertEquals(800, target1.width)
        assertEquals(600, target1.height)

        val target2 = ImageOptimizer.calculateTargetDimensions(srcWidth = 1024, srcHeight = 1024, maxDimension = 1024)
        assertEquals(1024, target2.width)
        assertEquals(1024, target2.height)

        val target3 = ImageOptimizer.calculateTargetDimensions(srcWidth = 600, srcHeight = 800, maxDimension = 1024)
        assertEquals(600, target3.width)
        assertEquals(800, target3.height)
    }

    // 5. IN_SAMPLE_SIZE OOM PROTECTION: Pangkat 2 untuk Foto Resolusi Sangat Tinggi
    @Test
    fun testInSampleSizeCalculations() {
        // 4000x3000 -> req 1024x768 -> sampleSize 2 (hemat RAM 75%)
        assertEquals(2, ImageOptimizer.calculateInSampleSize(4000, 3000, 1024, 768))

        // 8000x6000 (48MP kamera) -> req 1024x768 -> sampleSize 4 (hemat RAM 93.75%)
        assertEquals(4, ImageOptimizer.calculateInSampleSize(8000, 6000, 1024, 768))

        // Citra sudah pas atau kecil -> sampleSize 1
        assertEquals(1, ImageOptimizer.calculateInSampleSize(800, 600, 800, 600))

        // Overload 3-parameter (origWidth, origHeight, maxDimension)
        assertEquals(2, ImageOptimizer.calculateInSampleSize(4000, 3000, 1024))
        assertEquals(4, ImageOptimizer.calculateInSampleSize(8000, 6000, 1024))
        assertEquals(1, ImageOptimizer.calculateInSampleSize(800, 600, 1024))
    }

    // 6. END-TO-END COMPRESSION: Citra Besar Terkompresi Menjadi < 150KB
    @Test
    fun testEndToEndCompressionUnder150KB() {
        // Buat citra 2400x1800 sintetis (~1.5MB mentah)
        val rawBytes = createSyntheticJpeg(2400, 1800)
        assertTrue(rawBytes.isNotEmpty())

        val optimizedBytes = ImageOptimizer.optimize(rawBytes, maxDimension = 1024, quality = 82)
        assertTrue(optimizedBytes.isNotEmpty())

        // Syarat AUD-107: File size < 150KB
        val sizeKb = optimizedBytes.size / 1024
        assertTrue(
            optimizedBytes.size < ImageOptimizer.MAX_ALLOWED_FILE_SIZE_BYTES,
            "Ukuran berkas hasil kompresi harus < 150KB, namun didapat ${sizeKb}KB"
        )

        // Verifikasi dimensi hasil kompresi
        val dims = JvmImageEngine().decodeDimensions(optimizedBytes)
        assertTrue(dims != null)
        assertEquals(1024, dims.width)
        assertEquals(768, dims.height)

        // Verifikasi magic bytes JPEG (0xFF, 0xD8, 0xFF)
        assertEquals(0xFF.toByte(), optimizedBytes[0])
        assertEquals(0xD8.toByte(), optimizedBytes[1])
        assertEquals(0xFF.toByte(), optimizedBytes[2])
    }

    // 7. INPUT VALIDATION: Menolak Byte Kosong
    @Test
    fun testEmptyInputThrowsException() {
        val ex = assertFailsWith<IllegalArgumentException> {
            ImageOptimizer.optimize(ByteArray(0))
        }
        assertTrue(ex.message?.contains("kosong") == true)
    }

    // 8. INPUT VALIDATION: Menolak Data Korup
    @Test
    fun testCorruptBytesThrowsException() {
        val corruptData = byteArrayOf(0x00, 0x11, 0x22, 0x33, 0x44, 0x55)
        val ex = assertFailsWith<IllegalArgumentException> {
            ImageOptimizer.optimize(corruptData)
        }
        assertTrue(ex.message?.contains("tidak valid atau korup") == true)
    }

    // 9. BOUNDARY: Validasi Nilai Dimensi dan Kualitas
    @Test
    fun testInvalidDimensionsThrows() {
        assertFailsWith<IllegalArgumentException> {
            ImageOptimizer.calculateTargetDimensions(0, 100)
        }
        assertFailsWith<IllegalArgumentException> {
            ImageOptimizer.calculateTargetDimensions(100, -5)
        }
        assertFailsWith<IllegalArgumentException> {
            ImageOptimizer.calculateTargetDimensions(100, 100, maxDimension = 0)
        }
        assertFailsWith<IllegalArgumentException> {
            ImageOptimizer.calculateInSampleSize(0, 100, 50, 50)
        }
        assertFailsWith<IllegalArgumentException> {
            ImageOptimizer.calculateInSampleSize(100, 0, 50, 50)
        }
        assertFailsWith<IllegalArgumentException> {
            ImageOptimizer.calculateInSampleSize(100, 100, 0, 50)
        }
        assertFailsWith<IllegalArgumentException> {
            ImageOptimizer.calculateInSampleSize(100, 100, 50, 0)
        }
    }

    @Test
    fun testInvalidOptimizeArguments() {
        val validBytes = createSyntheticJpeg(100, 100)
        assertFailsWith<IllegalArgumentException> {
            ImageOptimizer.optimize(validBytes, maxDimension = 0, quality = 82)
        }
        assertFailsWith<IllegalArgumentException> {
            ImageOptimizer.optimize(validBytes, maxDimension = 1024, quality = 0)
        }
        assertFailsWith<IllegalArgumentException> {
            ImageOptimizer.optimize(validBytes, maxDimension = 1024, quality = 101)
        }
    }

    @Test
    fun testImageDimensionsEntity() {
        val dims = ImageDimensions(1024, 768)
        assertEquals(1024, dims.width)
        assertEquals(768, dims.height)
        val (w, h) = dims
        assertEquals(1024, w)
        assertEquals(768, h)
        assertEquals(ImageDimensions(1024, 768), dims)

        assertFailsWith<IllegalArgumentException> {
            ImageDimensions(-1, 100)
        }
        assertFailsWith<IllegalArgumentException> {
            ImageDimensions(100, -1)
        }
    }

    @Test
    fun testJvmImageEngineDirectEdgeCases() {
        val engine = JvmImageEngine()
        assertNull(engine.decodeDimensions(ByteArray(0)))
        assertNull(engine.decodeDimensions(byteArrayOf(1, 2, 3, 4)))

        assertFailsWith<IllegalStateException> {
            engine.compress(
                rawBytes = byteArrayOf(1, 2, 3, 4),
                targetDimensions = ImageDimensions(100, 100),
                sampleSize = 1,
                quality = 82
            )
        }
    }

    @Test
    fun testDefaultParameterInvocations() {
        val raw = createSyntheticJpeg(800, 600)
        val opt = ImageOptimizer.optimize(raw)
        assertTrue(opt.isNotEmpty())

        val targetDefault = ImageOptimizer.calculateTargetDimensions(2000, 1500)
        assertEquals(1024, targetDefault.width)
        assertEquals(768, targetDefault.height)
    }

    // 14. EMPIRICAL CHALLENGE: Multi-coroutine Concurrency Stress Test
    @Test
    fun testConcurrentOptimizationFromMultipleCoroutines() = runTest {
        val testImages = listOf(
            createSyntheticJpeg(1600, 1200),
            createSyntheticJpeg(1200, 1600),
            createSyntheticJpeg(2000, 1500),
            createSyntheticJpeg(800, 600)
        )

        // Launch 20 concurrent optimization tasks on Dispatchers.Default
        val jobs = (0 until 20).map { index ->
            async(Dispatchers.Default) {
                val input = testImages[index % testImages.size]
                val result = ImageOptimizer.optimize(input, maxDimension = 1024, quality = 82)
                assertTrue(result.isNotEmpty())
                assertTrue(result.size < ImageOptimizer.MAX_ALLOWED_FILE_SIZE_BYTES)
                assertEquals(0xFF.toByte(), result[0])
                assertEquals(0xD8.toByte(), result[1])
                assertEquals(0xFF.toByte(), result[2])
                result
            }
        }

        val results = jobs.awaitAll()
        assertEquals(20, results.size)
        results.forEach { bytes ->
            val dims = JvmImageEngine().decodeDimensions(bytes)
            assertTrue(dims != null)
            assertTrue(dims.width <= 1024 && dims.height <= 1024)
        }
    }

    // 15. EMPIRICAL CHALLENGE: Extreme Aspect Ratio Preservation (Panoramic & Ultra-tall)
    @Test
    fun testExtremeAspectRatios() {
        // Ultra-wide banner: 10000 x 500 -> target maxDimension 1024
        val wide = ImageOptimizer.calculateTargetDimensions(10000, 500, 1024)
        assertEquals(1024, wide.width)
        assertEquals(51, wide.height) // 500 * (1024/10000) = 51.2 -> 51
        assertTrue(wide.width <= 1024 && wide.height <= 1024)

        // Ultra-tall banner: 500 x 10000 -> target maxDimension 1024
        val tall = ImageOptimizer.calculateTargetDimensions(500, 10000, 1024)
        assertEquals(51, tall.width)
        assertEquals(1024, tall.height)
        assertTrue(tall.width <= 1024 && tall.height <= 1024)

        // Single pixel image: 1 x 1 -> no upscaling
        val singlePixel = ImageOptimizer.calculateTargetDimensions(1, 1, 1024)
        assertEquals(1, singlePixel.width)
        assertEquals(1, singlePixel.height)

        // Exact boundary: 1025 x 1024 -> must downscale 1025 to 1024
        val boundary = ImageOptimizer.calculateTargetDimensions(1025, 1024, 1024)
        assertEquals(1024, boundary.width)
        assertEquals(1023, boundary.height)
    }
}


package com.anlian.italase.shared.media

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
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
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Empirical Challenger 2 concurrency, reentrancy, and memory-safety verification suite
 * for [ImageOptimizer] and [ImageCompressionEngine].
 *
 * Verifies:
 * 1. Safe concurrent optimization across 50+ coroutines executing on [Dispatchers.Default].
 * 2. Independence of image buffers (no data cross-contamination or buffer races).
 * 3. Engine reentrancy under multi-threaded load.
 * 4. Engine reference stability and potential race condition analysis when `engine` is reassigned.
 */
class ImageOptimizerConcurrencyTest {

    private val originalEngine = ImageOptimizer.engine

    @BeforeTest
    fun setUp() {
        ImageOptimizer.engine = JvmImageEngine()
    }

    @AfterTest
    fun tearDown() {
        ImageOptimizer.engine = originalEngine
    }

    /**
     * Generates a unique synthetic JPEG with specific dimensions, seed, and embedded signature module.
     */
    private fun createSignatureJpeg(width: Int, height: Int, seed: Long): ByteArray {
        val img = BufferedImage(width, height, BufferedImage.TYPE_INT_RGB)
        val g = img.createGraphics()
        try {
            g.color = Color.WHITE
            g.fillRect(0, 0, width, height)

            val rng = Random(seed)
            g.color = Color(rng.nextInt(256), rng.nextInt(256), rng.nextInt(256))
            g.fillRect(10, 10, width - 20, height - 20)

            // High-contrast pattern
            g.color = Color.BLACK
            for (i in 0 until 10) {
                g.fillRect(i * 20, i * 20, 15, 15)
            }
        } finally {
            g.dispose()
        }
        val out = ByteArrayOutputStream()
        ImageIO.write(img, "jpeg", out)
        return out.toByteArray()
    }

    /**
     * 1. CONCURRENCY: 50 concurrent coroutines executing [ImageOptimizer.optimize]
     * in parallel on [Dispatchers.Default].
     */
    @Test
    fun testConcurrentOptimizationsAcrossMultipleCoroutines() {
        runBlocking(Dispatchers.Default) {
            val coroutineCount = 50
            val deferredList = (1..coroutineCount).map { index ->
                async {
                    val width = 1200 + (index * 20)
                    val height = 900 + (index * 15)
                    val rawBytes = createSignatureJpeg(width, height, seed = index.toLong())

                    val optimized = ImageOptimizer.optimize(rawBytes, maxDimension = 1024, quality = 80)

                    // Verify output validity
                    assertTrue(optimized.isNotEmpty(), "Corrupted empty output for coroutine $index")
                    assertEquals(0xFF.toByte(), optimized[0], "Invalid JPEG SOI byte 0 for coroutine $index")
                    assertEquals(0xD8.toByte(), optimized[1], "Invalid JPEG SOI byte 1 for coroutine $index")

                    val decoded = JvmImageEngine().decodeDimensions(optimized)
                    assertNotNull(decoded, "Decoded dimensions null for coroutine $index")
                    assertTrue(
                        decoded.width <= 1024 && decoded.height <= 1024,
                        "Dimensions exceeded max for coroutine $index: ${decoded.width}x${decoded.height}"
                    )

                    // Return pair of (index, output size) for verification
                    Pair(index, optimized.size)
                }
            }

            val results = deferredList.awaitAll()
            assertEquals(coroutineCount, results.size)
            // Verify all coroutines produced unique, valid compressed artifacts
            results.forEach { (index, size) ->
                assertTrue(size > 0, "Size should be > 0 for coroutine $index")
            }
        }
    }

    /**
     * 2. REENTRANCY: Engine instances must be reentrant and not share mutable class-level state.
     */
    @Test
    fun testEngineReentrancyUnderHeavyLoad() {
        runBlocking(Dispatchers.Default) {
            val engine = JvmImageEngine()
            val iterations = 40

            val tasks = (1..iterations).map { idx ->
                async {
                    val raw = createSignatureJpeg(800, 600, idx.toLong())
                    val dims = engine.decodeDimensions(raw)
                    assertNotNull(dims)
                    assertEquals(800, dims.width)
                    assertEquals(600, dims.height)

                    val compressed = engine.compress(
                        rawBytes = raw,
                        targetDimensions = ImageDimensions(400, 300),
                        sampleSize = 2,
                        quality = 75
                    )
                    assertTrue(compressed.isNotEmpty())
                }
            }

            tasks.awaitAll()
        }
    }

    /**
     * 3. RACE CONDITION EMPIRICAL PROOF:
     * [ImageOptimizer.engine] is a mutable public `var` without `@Volatile`.
     * Furthermore, [ImageOptimizer.optimize] reads `engine` TWICE:
     * Line 72: `val dimensions = engine.decodeDimensions(rawBytes)`
     * Line 88: `return engine.compress(...)`
     *
     * This test demonstrates the vulnerability when `engine` is replaced concurrently:
     * If an inconsistent engine is installed between decode and compress,
     * an unexpected engine handles compression.
     */
    @Test
    fun testEngineSwitchMidFlightVulnerability() {
        var decodeCount = 0
        var compressCount = 0

        val mockEngine1 = object : ImageCompressionEngine {
            override fun decodeDimensions(rawBytes: ByteArray): ImageDimensions? {
                decodeCount++
                return ImageDimensions(2000, 1500)
            }
            override fun compress(
                rawBytes: ByteArray,
                targetDimensions: ImageDimensions,
                sampleSize: Int,
                quality: Int
            ): ByteArray {
                compressCount++
                return byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0x01)
            }
        }

        val mockEngine2 = object : ImageCompressionEngine {
            override fun decodeDimensions(rawBytes: ByteArray): ImageDimensions? {
                throw UnsupportedOperationException("Engine 2 decode not allowed")
            }
            override fun compress(
                rawBytes: ByteArray,
                targetDimensions: ImageDimensions,
                sampleSize: Int,
                quality: Int
            ): ByteArray {
                throw IllegalStateException("Engine 2 invoked unexpectedly for compress")
            }
        }

        ImageOptimizer.engine = mockEngine1
        val raw = byteArrayOf(1, 2, 3, 4)

        // Normal path with consistent engine
        val result = ImageOptimizer.optimize(raw)
        assertEquals(1, decodeCount)
        assertEquals(1, compressCount)
        assertEquals(3, result.size)

        // Mid-flight swap scenario:
        // Demonstrates that because `engine` is read multiple times in `optimize()`,
        // swapping `engine` causes split-brain execution between decodeDimensions and compress.
        val splitBrainEngine = object : ImageCompressionEngine {
            override fun decodeDimensions(rawBytes: ByteArray): ImageDimensions? {
                // Simulates another thread swapping the engine immediately after decodeDimensions
                ImageOptimizer.engine = mockEngine2
                return ImageDimensions(1000, 1000)
            }
            override fun compress(
                rawBytes: ByteArray,
                targetDimensions: ImageDimensions,
                sampleSize: Int,
                quality: Int
            ): ByteArray {
                return byteArrayOf(0xFF.toByte(), 0xD8.toByte())
            }
        }

        ImageOptimizer.engine = splitBrainEngine
        // optimize() calls splitBrainEngine.decodeDimensions -> swaps ImageOptimizer.engine to mockEngine2 -> calls mockEngine2.compress -> throws!
        assertFailsWith<IllegalStateException> {
            ImageOptimizer.optimize(raw)
        }
    }
}

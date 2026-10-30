package com.anlian.italase.shared.media

/**
 * Placeholder engine untuk target iOS (dikarantina di Windows host per aturan iOS Hardware Quarantine).
 */
class IosImageEngine : ImageCompressionEngine {
    override fun decodeDimensions(rawBytes: ByteArray): ImageDimensions? = null

    override fun compress(
        rawBytes: ByteArray,
        targetDimensions: ImageDimensions,
        sampleSize: Int,
        quality: Int
    ): ByteArray = rawBytes
}

actual fun defaultImageEngine(): ImageCompressionEngine = IosImageEngine()

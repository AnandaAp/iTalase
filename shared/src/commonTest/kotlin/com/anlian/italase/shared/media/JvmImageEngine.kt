package com.anlian.italase.shared.media

import java.awt.Image
import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import javax.imageio.IIOImage
import javax.imageio.ImageIO
import javax.imageio.ImageWriteParam
import javax.imageio.ImageWriter

/**
 * Mesin kompresi JVM untuk test host (testAndroidHostTest).
 * Melakukan kompresi citra dan downsampling sungguhan di lingkungan JVM.
 */
class JvmImageEngine : ImageCompressionEngine {

    override fun decodeDimensions(rawBytes: ByteArray): ImageDimensions? {
        if (rawBytes.isEmpty()) return null
        return try {
            ByteArrayInputStream(rawBytes).use { stream ->
                val img = ImageIO.read(stream) ?: return null
                ImageDimensions(img.width, img.height)
            }
        } catch (_: Throwable) {
            null
        }
    }

    override fun compress(
        rawBytes: ByteArray,
        targetDimensions: ImageDimensions,
        sampleSize: Int,
        quality: Int
    ): ByteArray {
        val srcImage = ByteArrayInputStream(rawBytes).use { ImageIO.read(it) }
            ?: throw IllegalStateException("Gagal membaca citra di JVM")

        val resized = BufferedImage(
            targetDimensions.width,
            targetDimensions.height,
            BufferedImage.TYPE_INT_RGB
        )
        val g = resized.createGraphics()
        try {
            g.drawImage(
                srcImage.getScaledInstance(targetDimensions.width, targetDimensions.height, Image.SCALE_SMOOTH),
                0,
                0,
                null
            )
        } finally {
            g.dispose()
        }

        val outStream = ByteArrayOutputStream()
        val writer: ImageWriter = ImageIO.getImageWritersByFormatName("jpeg").next()
        val iioStream = ImageIO.createImageOutputStream(outStream)
        writer.output = iioStream

        val params: ImageWriteParam = writer.defaultWriteParam.apply {
            compressionMode = ImageWriteParam.MODE_EXPLICIT
            compressionQuality = (quality.coerceIn(1, 100) / 100f)
        }

        writer.write(null, IIOImage(resized, null, null), params)
        writer.dispose()
        iioStream.close()

        return outStream.toByteArray()
    }
}

package com.anlian.italase.shared.media

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream

/**
 * Mesin kompresi Android native berbasis BitmapFactory dan Bitmap:
 * 1. Two-pass decoding via BitmapFactory.Options (inJustDecodeBounds -> inSampleSize).
 * 2. Normalisasi rotasi berdasarkan metadata EXIF (ExifInterface).
 * 3. Exact bilinear scaling (Bitmap.createScaledBitmap).
 * 4. Kompresi JPEG dengan pembersihan memori instan (Bitmap.recycle).
 */
class AndroidImageEngine : ImageCompressionEngine {

    override fun decodeDimensions(rawBytes: ByteArray): ImageDimensions? {
        if (rawBytes.isEmpty()) return null
        return try {
            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            BitmapFactory.decodeByteArray(rawBytes, 0, rawBytes.size, options)
            if (options.outWidth > 0 && options.outHeight > 0) {
                val orientation = getExifOrientation(rawBytes)
                if (orientation == 90 || orientation == 270) {
                    ImageDimensions(options.outHeight, options.outWidth)
                } else {
                    ImageDimensions(options.outWidth, options.outHeight)
                }
            } else {
                null
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
        val options = BitmapFactory.Options().apply {
            inSampleSize = sampleSize.coerceAtLeast(1)
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }
        val sampledBitmap = BitmapFactory.decodeByteArray(rawBytes, 0, rawBytes.size, options)
            ?: throw IllegalStateException("Gagal melakukan decoding bitmap")

        val orientedBitmap = applyExifOrientation(sampledBitmap, rawBytes)

        val scaledBitmap = if (orientedBitmap.width != targetDimensions.width || orientedBitmap.height != targetDimensions.height) {
            Bitmap.createScaledBitmap(
                orientedBitmap,
                targetDimensions.width,
                targetDimensions.height,
                true
            ).also {
                if (it !== orientedBitmap) {
                    orientedBitmap.recycle()
                }
            }
        } else {
            orientedBitmap
        }

        val outputStream = ByteArrayOutputStream()
        try {
            scaledBitmap.compress(Bitmap.CompressFormat.JPEG, quality.coerceIn(1, 100), outputStream)
            return outputStream.toByteArray()
        } finally {
            scaledBitmap.recycle()
        }
    }

    private fun getExifOrientation(rawBytes: ByteArray): Int {
        return try {
            val exif = ExifInterface(ByteArrayInputStream(rawBytes))
            when (exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
                ExifInterface.ORIENTATION_ROTATE_90 -> 90
                ExifInterface.ORIENTATION_ROTATE_180 -> 180
                ExifInterface.ORIENTATION_ROTATE_270 -> 270
                else -> 0
            }
        } catch (_: Throwable) {
            0
        }
    }

    private fun applyExifOrientation(bitmap: Bitmap, rawBytes: ByteArray): Bitmap {
        val rotationDegrees = getExifOrientation(rawBytes).toFloat()
        if (rotationDegrees == 0f) return bitmap

        return try {
            val matrix = Matrix().apply { postRotate(rotationDegrees) }
            val rotated = Bitmap.createBitmap(
                bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true
            )
            if (rotated !== bitmap) {
                bitmap.recycle()
            }
            rotated
        } catch (_: Throwable) {
            bitmap
        }
    }
}

actual fun defaultImageEngine(): ImageCompressionEngine = AndroidImageEngine()

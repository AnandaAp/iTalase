package com.anlian.italase.shared.media

/**
 * Dimensi citra (lebar x tinggi) dalam piksel.
 */
data class ImageDimensions(val width: Int, val height: Int) {
    init {
        require(width >= 0 && height >= 0) { "Dimensi tidak boleh negatif: ${width}x$height" }
    }
}

/**
 * Kontrak mesin kompresi citra per platform.
 */
interface ImageCompressionEngine {
    /**
     * Membaca dimensi citra tanpa memuat seluruh pixel bitmap ke memori.
     * Mengembalikan null jika format tidak didukung atau byte citra korup.
     */
    fun decodeDimensions(rawBytes: ByteArray): ImageDimensions?

    /**
     * Melakukan downscaling ke targetDimensions dan mengompres ke format JPEG.
     */
    fun compress(
        rawBytes: ByteArray,
        targetDimensions: ImageDimensions,
        sampleSize: Int,
        quality: Int
    ): ByteArray
}

/**
 * Engine optimasi citra untuk QRIS dan lampiran kasir (AUD-107).
 *
 * Menerapkan:
 * 1. Validasi input: menolak byte kosong atau korup.
 * 2. Downsampling: membatasi resolusi maksimum ke 1024x1024 sambil mempertahankan rasio aspek.
 * 3. Kalkulasi inSampleSize memory-safe: mencegah OutOfMemoryError saat decoding foto besar (50MP).
 * 4. Kompresi JPEG dengan kualitas terkalibrasi (default 82%) untuk target file size < 150KB.
 */
object ImageOptimizer {

    const val DEFAULT_MAX_DIMENSION = 1024
    const val DEFAULT_JPEG_QUALITY = 82
    const val MAX_ALLOWED_FILE_SIZE_BYTES = 150 * 1024 // 150 KB

    /**
     * Mesin kompresi aktif. Di Android didelegasikan ke AndroidImageEngine.
     * Dapat dioverride dalam pengujian unit dengan JvmImageEngine atau test double lainnya.
     */
    var engine: ImageCompressionEngine = defaultImageEngine()

    /**
     * Mengoptimasi citra mentah menjadi JPEG terkompresi beresolusi <= maxDimension x maxDimension.
     *
     * @param rawBytes Data biner citra sumber.
     * @param maxDimension Batas piksel dimensi terpanjang (default 1024).
     * @param quality Kualitas kompresi JPEG 1..100 (default 82).
     * @return ByteArray JPEG teroptimasi, biasanya berkuran < 150KB.
     * @throws IllegalArgumentException jika masukan kosong, korup, atau dimensi/kualitas tidak valid.
     */
    fun optimize(
        rawBytes: ByteArray,
        maxDimension: Int = DEFAULT_MAX_DIMENSION,
        quality: Int = DEFAULT_JPEG_QUALITY
    ): ByteArray {
        require(rawBytes.isNotEmpty()) { "Citra mentah tidak boleh kosong (0 bytes)" }
        require(maxDimension > 0) { "Maksimal dimensi harus positif: $maxDimension" }
        require(quality in 1..100) { "Kualitas harus antara 1 dan 100: $quality" }

        val dimensions = engine.decodeDimensions(rawBytes)
            ?: throw IllegalArgumentException("Format berkas citra tidak valid atau korup")

        val targetDimensions = calculateTargetDimensions(
            srcWidth = dimensions.width,
            srcHeight = dimensions.height,
            maxDimension = maxDimension
        )

        val sampleSize = calculateInSampleSize(
            srcWidth = dimensions.width,
            srcHeight = dimensions.height,
            reqWidth = targetDimensions.width,
            reqHeight = targetDimensions.height
        )

        return engine.compress(
            rawBytes = rawBytes,
            targetDimensions = targetDimensions,
            sampleSize = sampleSize,
            quality = quality.coerceIn(1, 100)
        )
    }

    /**
     * Menghitung resolusi target dengan mempertahankan rasio aspek asli (aspect ratio).
     * Jika citra sudah lebih kecil atau sama dengan batas dimensi, dimensi asli dipertahankan (tidak di-upscale).
     */
    fun calculateTargetDimensions(
        srcWidth: Int,
        srcHeight: Int,
        maxDimension: Int = DEFAULT_MAX_DIMENSION
    ): ImageDimensions {
        require(srcWidth > 0 && srcHeight > 0) { "Dimensi sumber harus positif: ${srcWidth}x$srcHeight" }
        require(maxDimension > 0) { "Maksimal dimensi harus positif: $maxDimension" }

        // Jika citra sudah lebih kecil atau sama dengan batas dimensi, pertahankan ukuran asli
        if (srcWidth <= maxDimension && srcHeight <= maxDimension) {
            return ImageDimensions(srcWidth, srcHeight)
        }

        val widthRatio = maxDimension.toDouble() / srcWidth
        val heightRatio = maxDimension.toDouble() / srcHeight
        val scale = minOf(widthRatio, heightRatio)

        val targetWidth = (srcWidth * scale).toInt().coerceAtLeast(1)
        val targetHeight = (srcHeight * scale).toInt().coerceAtLeast(1)

        return ImageDimensions(targetWidth, targetHeight)
    }

    /**
     * Menghitung nilai inSampleSize optimal (pangkat 2) untuk decoding bertahap
     * guna menghindari OutOfMemoryError pada perangkat POS berspesifikasi rendah.
     */
    fun calculateInSampleSize(
        srcWidth: Int,
        srcHeight: Int,
        reqWidth: Int,
        reqHeight: Int
    ): Int {
        require(srcWidth > 0 && srcHeight > 0) { "Dimensi sumber harus positif: ${srcWidth}x$srcHeight" }
        require(reqWidth > 0 && reqHeight > 0) { "Dimensi target harus positif: ${reqWidth}x$reqHeight" }

        var inSampleSize = 1
        if (srcHeight > reqHeight || srcWidth > reqWidth) {
            val halfHeight = srcHeight / 2
            val halfWidth = srcWidth / 2

            while ((halfHeight / inSampleSize) >= reqHeight && (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2
            }
        }
        return inSampleSize
    }

    /**
     * Overload helper untuk menghitung inSampleSize langsung dari dimensi asli dan batas maksimum.
     */
    fun calculateInSampleSize(
        origWidth: Int,
        origHeight: Int,
        maxDimension: Int
    ): Int {
        val target = calculateTargetDimensions(origWidth, origHeight, maxDimension)
        return calculateInSampleSize(origWidth, origHeight, target.width, target.height)
    }
}

/**
 * Menyediakan implementasi default per platform.
 */
expect fun defaultImageEngine(): ImageCompressionEngine

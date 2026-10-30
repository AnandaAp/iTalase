package com.anlian.italase.shared.data.local.entity

import androidx.compose.runtime.Immutable
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.anlian.italase.shared.data.local.Tables

/**
 * Konfigurasi QRIS kasir untuk metode pembayaran nontunai.
 *
 * Disimpan sebagai baris tunggal (singleton row) dengan `id = 1` di tabel [Tables.QRIS_CONFIG].
 * [imageBytes] menyimpan citra barcode QRIS teroptimasi (< 150KB) dalam format JPEG/PNG.
 * [promptDismissCount] mencatat berapa kali kasir mengabaikan pop-up inisiasi QRIS (maksimal 3x).
 */
@Immutable
@Entity(tableName = Tables.QRIS_CONFIG)
data class QrisConfigEntity(
    /** ID baris tunggal (selalu 1). */
    @PrimaryKey val id: Int = 1,
    /** Apakah metode QRIS aktif dan ditampilkan pada dialog pembayaran kasir. */
    val isEnabled: Boolean = false,
    /** Binary blob citra QRIS terkompresi; null jika belum diunggah. */
    val imageBytes: ByteArray? = null,
    /** MIME type citra, default "image/jpeg". */
    val mimeType: String = "image/jpeg",
    /** Jumlah pengabaian prompt inisiasi di kasir; >= 3 menghentikan prompt permanen. */
    val promptDismissCount: Int = 0,
    /** Epoch millis pembaruan terakhir. */
    val updatedAt: Long = 0L,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as QrisConfigEntity

        if (id != other.id) return false
        if (isEnabled != other.isEnabled) return false
        if (imageBytes != null) {
            if (other.imageBytes == null) return false
            if (!imageBytes.contentEquals(other.imageBytes)) return false
        } else if (other.imageBytes != null) return false
        if (mimeType != other.mimeType) return false
        if (promptDismissCount != other.promptDismissCount) return false
        if (updatedAt != other.updatedAt) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id
        result = 31 * result + isEnabled.hashCode()
        result = 31 * result + (imageBytes?.contentHashCode() ?: 0)
        result = 31 * result + mimeType.hashCode()
        result = 31 * result + promptDismissCount
        result = 31 * result + updatedAt.hashCode()
        return result
    }

    override fun toString(): String {
        val bytesInfo = if (imageBytes != null) "${imageBytes.size} bytes" else "null"
        return "QrisConfigEntity(id=$id, isEnabled=$isEnabled, imageBytes=$bytesInfo, mimeType='$mimeType', promptDismissCount=$promptDismissCount, updatedAt=$updatedAt)"
    }
}

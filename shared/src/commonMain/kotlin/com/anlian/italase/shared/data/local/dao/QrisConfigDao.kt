package com.anlian.italase.shared.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.anlian.italase.shared.data.local.Tables
import com.anlian.italase.shared.data.local.entity.QrisConfigEntity
import kotlinx.coroutines.flow.Flow

/**
 * Akses tabel qris_config (baris tunggal id = 1).
 * Mengatur pembacaan status QRIS, penyimpanan citra barcode, dan counter pengabaian prompt kasir.
 */
@Dao
interface QrisConfigDao {
    /** Mengamati konfigurasi QRIS secara reaktif untuk layar Kasir dan Pengaturan. */
    @Query("SELECT * FROM ${Tables.QRIS_CONFIG} WHERE id = 1 LIMIT 1")
    fun observeConfig(): Flow<QrisConfigEntity?>

    /** Mengambil konfigurasi QRIS saat ini sekali baca. */
    @Query("SELECT * FROM ${Tables.QRIS_CONFIG} WHERE id = 1 LIMIT 1")
    suspend fun getConfig(): QrisConfigEntity?

    /** Menyimpan atau memperbarui konfigurasi QRIS (id = 1). */
    @Upsert
    suspend fun upsert(config: QrisConfigEntity)

    /** Menambah hitungan pengabaian prompt banner di kasir. */
    @Query("UPDATE ${Tables.QRIS_CONFIG} SET promptDismissCount = promptDismissCount + 1 WHERE id = 1")
    suspend fun incrementDismissCount()

    /** Mengubah status aktif/nonaktif QRIS tanpa menghapus citra barcode. */
    @Query("UPDATE ${Tables.QRIS_CONFIG} SET isEnabled = :enabled WHERE id = 1")
    suspend fun setEnabled(enabled: Boolean)

    /** Menghapus konfigurasi QRIS dari database. */
    @Query("DELETE FROM ${Tables.QRIS_CONFIG} WHERE id = 1")
    suspend fun deleteConfig()
}

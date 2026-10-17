package com.anlian.italase.shared.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.anlian.italase.shared.data.local.Tables
import com.anlian.italase.shared.data.local.entity.ExpenseEntity
import kotlinx.coroutines.flow.Flow

/** Akses tabel expenses. Daftarnya dipaginasi dari sisi pemanggil dengan LIMIT
 *  yang bertumbuh (lihat util/Paging.kt), jadi tidak ada varian OFFSET. */
@Dao
interface ExpenseDao {
    // LIMIT bertumbuh dari sisi pemanggil (lihat util/Paging.kt): awalnya 20
    // baris, lalu 40, 60, ... Jadi awalnya Room hanya mengirim satu halaman.
    @Query("SELECT * FROM ${Tables.EXPENSES} ORDER BY dateMillis DESC LIMIT :limit")
    fun observePage(limit: Int): Flow<List<ExpenseEntity>>

    /** Pengeluaran dalam rentang millis, urut terbaru; dasar laporan harian. */
    @Query("SELECT * FROM ${Tables.EXPENSES} WHERE dateMillis BETWEEN :from AND :to ORDER BY dateMillis DESC")
    suspend fun listRange(from: Long, to: Long): List<ExpenseEntity>

    /** Total pengeluaran rentang; 0 kalau tidak ada baris (IFNULL). */
    @Query("SELECT IFNULL(SUM(amount),0) FROM ${Tables.EXPENSES} WHERE dateMillis BETWEEN :from AND :to")
    suspend fun sumRange(from: Long, to: Long): Long

    /** id 0 berarti baris baru, selain itu menimpa baris dengan id tersebut. */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(e: ExpenseEntity): Long

    /** Hapus permanen satu baris pengeluaran. */
    @Query("DELETE FROM ${Tables.EXPENSES} WHERE id = :id")
    suspend fun delete(id: Long)
}

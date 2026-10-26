package com.anlian.italase.shared.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.anlian.italase.shared.data.local.Tables
import com.anlian.italase.shared.data.local.entity.DebtEntity
import com.anlian.italase.shared.data.local.entity.DebtPaymentEntity
import com.anlian.italase.shared.data.local.entity.DebtStatus
import kotlinx.coroutines.flow.Flow

/** Akses debts dan debt_payments. Pelunasan bukan satu operasi di sini:
 *  ShopRepository menulis cicilannya lebih dulu, lalu memperbarui ringkasan
 *  bon supaya keduanya tetap konsisten. */
@Dao
interface DebtDao {
    /** Semua bon — aktif maupun lunas — urut terbaru. */
    @Query("SELECT * FROM ${Tables.DEBTS} ORDER BY dateMillis DESC")
    fun observeAll(): Flow<List<DebtEntity>>

    /** Hanya bon yang masih ada sisanya. */
    @Query("SELECT * FROM ${Tables.DEBTS} WHERE status = '${DebtStatus.BELUM_LUNAS}' ORDER BY dateMillis DESC")
    fun observeActive(): Flow<List<DebtEntity>>

    /** Null kalau bon sudah tidak ada (mis. dihapus dari layar lain). */
    @Query("SELECT * FROM ${Tables.DEBTS} WHERE id = :id")
    suspend fun getById(id: Long): DebtEntity?

    /** Bon yang terhubung ke satu struk penjualan tertentu. */
    @Query("SELECT * FROM ${Tables.DEBTS} WHERE saleId = :saleId LIMIT 1")
    suspend fun getBySaleId(saleId: Long): DebtEntity?

    /** Mengembalikan id bon baru; id di dalam objek diabaikan generator. */
    @Insert
    suspend fun insert(debt: DebtEntity): Long

    /** Menimpa bon dengan id yang sama; dipakai saat menghitung ulang sisanya. */
    @Update
    suspend fun update(debt: DebtEntity)

    /** Total sisa seluruh bon aktif, untuk angka "piutang" di layar ringkasan. */
    @Query("SELECT IFNULL(SUM(remaining),0) FROM ${Tables.DEBTS} WHERE status = '${DebtStatus.BELUM_LUNAS}'")
    suspend fun totalPiutang(): Long

    /** Mencatat satu cicilan; TIDAK mengubah ringkasan bon di tabel debts. */
    @Insert
    suspend fun insertPayment(p: DebtPaymentEntity): Long

    /** Menghapus bon saja; baris debt_payments miliknya dibiarkan karena tidak
     *  ada FK cascade. */
    @Query("DELETE FROM ${Tables.DEBTS} WHERE id = :id")
    suspend fun delete(id: Long)
}

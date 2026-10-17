package com.anlian.italase.shared.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.anlian.italase.shared.data.local.Tables
import com.anlian.italase.shared.data.local.entity.SaleEntity
import com.anlian.italase.shared.data.local.entity.SaleItemEntity
import kotlinx.coroutines.flow.Flow

/** Akses sales dan sale_items. Hanya menambah dan membaca — struk yang sudah
 *  tercatat tidak bisa dihapus atau diubah angkanya, koreksi hanya lewat
 *  [updateInfo]. */
@Dao
interface SaleDao {
    /** Struk terbaru untuk daftar riwayat; `limit` adalah batas jumlah baris,
     *  bukan offset, jadi menaikkannya berarti memuat ulang dari awal. */
    @Query("SELECT * FROM ${Tables.SALES} ORDER BY dateMillis DESC LIMIT :limit")
    fun observeRecent(limit: Int = 100): Flow<List<SaleEntity>>

    /** Struk dalam rentang millis inklusif, urut terbaru; dasar laporan harian. */
    @Query("SELECT * FROM ${Tables.SALES} WHERE dateMillis BETWEEN :from AND :to ORDER BY dateMillis DESC")
    suspend fun listRange(from: Long, to: Long): List<SaleEntity>

    /** Total omzet rentang; 0 kalau tidak ada struk. */
    @Query("SELECT IFNULL(SUM(total),0) FROM ${Tables.SALES} WHERE dateMillis BETWEEN :from AND :to")
    suspend fun sumOmzet(from: Long, to: Long): Long

    /** Total profit rentang; 0 kalau tidak ada struk. */
    @Query("SELECT IFNULL(SUM(profit),0) FROM ${Tables.SALES} WHERE dateMillis BETWEEN :from AND :to")
    suspend fun sumProfit(from: Long, to: Long): Long

    /** Mengembalikan id struk baru; id di dalam objek diabaikan generator. */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSale(sale: SaleEntity): Long

    /** Null kalau struk dengan id itu tidak ada. */
    @Query("SELECT * FROM ${Tables.SALES} WHERE id = :id")
    suspend fun getSale(id: Long): SaleEntity?

    /** Struk yang punya minimal satu baris rincian berkategori tersebut;
     *  satu struk bisa muncul di beberapa kategori. */
    @Query("SELECT * FROM ${Tables.SALES} WHERE id IN (SELECT DISTINCT saleId FROM ${Tables.SALE_ITEMS} WHERE category = :category) ORDER BY dateMillis DESC LIMIT :limit")
    fun observeSalesByCategory(category: String, limit: Int = 200): Flow<List<SaleEntity>>

    /** Kategori yang benar-benar terpakai di struk (bukan daftar master),
     *  dipakai mengisi pilihan filter riwayat. Baris berkategori kosong
     *  dari database versi lama sengaja dibuang. */
    @Query("SELECT DISTINCT category FROM ${Tables.SALE_ITEMS} WHERE category <> '' ORDER BY category ASC")
    fun observeItemCategories(): Flow<List<String>>

    /** Hanya nama pelanggan dan catatan; angka transaksi sengaja tidak bisa
     *  diedit supaya laporan tetap cocok dengan stok yang sudah terpotong. */
    @Query("UPDATE ${Tables.SALES} SET customerName = :customer, note = :note WHERE id = :id")
    suspend fun updateInfo(id: Long, customer: String, note: String)

    /** Menyisipkan seluruh rincian struk sekaligus; saleId tiap baris harus
     *  sudah diisi karena tabel ini tidak punya autoGenerate untuk relasinya. */
    @Insert
    suspend fun insertItems(items: List<SaleItemEntity>)

    /** Rincian struk: nama, kategori, dan harga salinan saat transaksi. */
    @Query("SELECT * FROM ${Tables.SALE_ITEMS} WHERE saleId = :saleId")
    suspend fun itemsOf(saleId: Long): List<SaleItemEntity>

    /** Maksimal 10 barang terlaris di rentang, urut qty terbanyak. Agregasi
     *  dikerjakan SQL supaya seluruh isi sale_items tidak perlu masuk memori. */
    @Query("SELECT productName, SUM(qty) as q, SUM(subtotal) as s FROM ${Tables.SALE_ITEMS} WHERE saleId IN (SELECT id FROM ${Tables.SALES} WHERE dateMillis BETWEEN :from AND :to) GROUP BY productName ORDER BY q DESC LIMIT 10")
    suspend fun topProducts(from: Long, to: Long): List<TopRow>
}

/** Satu baris hasil [SaleDao.topProducts]; alias kolom SQL (`q`, `s`) bukan
 *  penamaan yang enak dibaca, jadi sengaja diberi nama pendek di query dan
 *  dijelaskan di sini. */
data class TopRow(
    /** Nama barang; pengelompokan memakai nama, bukan productId, supaya barang
     *  yang sudah dihapus tetap terhitung. */
    val productName: String,
    /** Total qty terjual dalam rentang. */
    val q: Int,
    /** Total nilai penjualan (jumlah subtotal) dalam rentang. */
    val s: Long,
)

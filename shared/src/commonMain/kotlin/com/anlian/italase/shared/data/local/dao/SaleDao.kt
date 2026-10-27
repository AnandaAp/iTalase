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

    /**
     * Riwayat dalam rentang waktu `[from, to]`, urut terbaru dulu.
     *
     * `LIMIT` di sini sah karena urutannya kronologis: "n transaksi terbaru di
     * rentang itu". Untuk daftar yang diurut ulang di klien (produk), LIMIT tidak
     * boleh dipakai — lihat catatan di `util/Paging.kt`.
     */
    @Query(
        "SELECT * FROM ${Tables.SALES} WHERE dateMillis BETWEEN :from AND :to " +
                "ORDER BY dateMillis DESC LIMIT :limit",
    )
    fun observeBetween(from: Long, to: Long, limit: Int): Flow<List<SaleEntity>>

    /**
     * Sama seperti [observeBetween] tapi dibatasi pada transaksi yang memuat item
     * berkategori [category].
     */
    @Query(
        "SELECT * FROM ${Tables.SALES} WHERE dateMillis BETWEEN :from AND :to " +
                "AND id IN (SELECT DISTINCT saleId FROM ${Tables.SALE_ITEMS} WHERE category = :category) " +
                "ORDER BY dateMillis DESC LIMIT :limit",
    )
    fun observeBetweenByCategory(
        from: Long,
        to: Long,
        category: String,
        limit: Int,
    ): Flow<List<SaleEntity>>

    /** Struk dalam rentang millis inklusif, urut terbaru; dasar laporan harian. */
    @Query("SELECT * FROM ${Tables.SALES} WHERE dateMillis BETWEEN :from AND :to ORDER BY dateMillis DESC")
    suspend fun listRange(from: Long, to: Long): List<SaleEntity>

    /**
     * Agregat satu rentang tanggal sekaligus (omzet, profit, jumlah struk) dalam satu kali query tabel.
     * Menggabungkan sumOmzet, sumProfit, dan countRange menjadi satu query efisien.
     */
    @Query("SELECT IFNULL(SUM(total),0) AS omzet, IFNULL(SUM(profit),0) AS profit, COUNT(*) AS trxCount FROM ${Tables.SALES} WHERE dateMillis BETWEEN :from AND :to")
    suspend fun dayAggregate(from: Long, to: Long): DayAggregateRow

    /** Jumlah struk dalam rentang. */
    @Query("SELECT COUNT(*) FROM ${Tables.SALES} WHERE dateMillis BETWEEN :from AND :to")
    suspend fun countRange(from: Long, to: Long): Int

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

    /** Memperbarui informasi pembayaran struk (nama pelanggan, catatan, metode bayar,
     *  uang diterima, dan status) tanpa mengubah total transaksi maupun pergerakan stok. */
    @Query(
        "UPDATE ${Tables.SALES} SET customerName = :customer, note = :note, " +
                "payMethod = :payMethod, paidAmount = :paidAmount, tendered = :tendered, " +
                "status = :status WHERE id = :id",
    )
    suspend fun updatePaymentInfo(
        id: Long,
        customer: String,
        note: String,
        payMethod: String,
        paidAmount: Long,
        tendered: Long,
        status: String,
    )

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

/**
 * Agregat ringkasan penjualan harian (omzet, profit, trxCount) dari [SaleDao.dayAggregate].
 */
data class DayAggregateRow(
    val omzet: Long = 0,
    val profit: Long = 0,
    val trxCount: Int = 0,
)


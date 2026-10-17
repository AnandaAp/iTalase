package com.anlian.italase.shared.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.anlian.italase.shared.data.ShopDefaults
import com.anlian.italase.shared.data.local.Tables

/** Pengeluaran operasional toko (bukan pembelian stok). Dikurangkan dari
 *  profit di ringkasan harian dan tidak terhubung ke produk mana pun. */
@Entity(tableName = Tables.EXPENSES)
data class ExpenseEntity(
    /** 0 berarti baris baru; Room mengisi id saat insert. */
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    /** Nama pengeluaran, mis. "Beli kantong plastik"; wajib diisi. */
    val name: String = "",
    /** Kategori bebas berupa teks; kosong diisi [ShopDefaults.CATEGORY]. */
    val category: String = ShopDefaults.CATEGORY,
    /** Nominal Rupiah bulat, validasinya > 0. */
    val amount: Long = 0,
    /** Epoch millis pengeluaran dicatat; dasar filter laporan harian. */
    val dateMillis: Long = 0,
    /** Catatan bebas. */
    val note: String = "",
)

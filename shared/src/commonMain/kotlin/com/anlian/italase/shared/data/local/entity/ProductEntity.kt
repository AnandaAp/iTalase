package com.anlian.italase.shared.data.local.entity

import androidx.compose.runtime.Immutable
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.anlian.italase.shared.data.ProductCategories
import com.anlian.italase.shared.data.ShopDefaults
import com.anlian.italase.shared.data.local.Tables

/** Master barang. `stock` di sini adalah stok berjalan yang dikurangi tiap
 *  checkout, bukan angka stok awal. Menghapus barang TIDAK menghapus riwayat
 *  penjualan, karena sale_items menyimpan salinan nama dan harga. */
@Immutable
@Entity(
    tableName = Tables.PRODUCTS,
    indices = [
        Index("name"),
        Index("category", "name"),
    ],
)
data class ProductEntity(
    /** 0 berarti baris baru; Room mengisi id saat insert, dan repository
     *  memakai nilai ini untuk membedakan tambah vs edit. */
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    /** Nama barang; disalin ke baris struk saat checkout sehingga mengganti
     *  nama tidak mengubah struk lama. */
    val name: String,
    /** Boleh kosong; hanya untuk pencarian cepat di kasir, bukan kunci unik. */
    val barcode: String = "",
    /** Salah satu nilai [ProductCategories]; ikut disalin ke baris struk
     *  supaya laporan per kategori tidak berubah saat kategori barang diedit. */
    val category: String = ProductCategories.OTHER,
    /** Harga modal terakhir yang diketahui (bukan HPP rata-rata); dasar
     *  perhitungan profit saat checkout. */
    val buyPrice: Long = 0,
    /** Harga jual saat ini dalam Rupiah bulat. */
    val sellPrice: Long = 0,
    /** Stok berjalan; dikurangi di dalam transaksi database yang sama dengan
     *  insert struk, jadi tidak pernah negatif. */
    val stock: Int = 0,
    /** Satuan hanya untuk tampilan (pcs, kg, ...); tidak ikut dihitung. */
    val unit: String = ShopDefaults.UNIT,
    /** Ambang peringatan stok menipis khusus barang ini. 0 berarti tidak
     *  diatur, dan UI masih memakai StockRules.LOW_STOCK_FALLBACK. */
    val minStock: Int = 0,
    /** Epoch millis pembuatan baris; diisi repository dan TIDAK diubah saat edit. */
    val createdAt: Long = 0,
    /** Epoch millis perubahan terakhir; di-set repository pada setiap
     *  penyimpanan barang maupun penyesuaian stok. */
    val updatedAt: Long = 0,
)

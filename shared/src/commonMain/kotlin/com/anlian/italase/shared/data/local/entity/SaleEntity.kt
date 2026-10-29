package com.anlian.italase.shared.data.local.entity

import androidx.compose.runtime.Immutable
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.anlian.italase.shared.data.local.Tables

/** Nilai kolom sales.status. Disimpan sebagai teks agar laporan bisa dibaca
 *  langsung dari database tanpa tabel lookup. */
object TxnStatus {
    /** Dibayar penuh saat transaksi. */
    const val LUNAS: String = "LUNAS"

    /** Ada sisa yang dibukukan sebagai piutang (lihat DebtEntity). */
    const val BON: String = "BON"
}

/** Nilai kolom sales.payMethod. */
object PayMethod {
    /** Bayar tunai di kasir. */
    const val TUNAI: String = "TUNAI"

    /** Bayar sebagian atau belum bayar; sisanya menjadi piutang. */
    const val BON: String = "BON"

    /** Transfer bank/e-wallet; selalu tercatat lunas. */
    const val TRANSFER: String = "TRANSFER"
}

/** Header struk: satu baris per checkout. total dan profit disimpan sudah
 *  dihitung, jadi laporan harian tidak perlu menjumlahkan sale_items. */
@Immutable
@Entity(
    tableName = Tables.SALES,
    indices = [
        Index("dateMillis"),
        Index("dateMillis", "status"),
    ],
)
data class SaleEntity(
    /** 0 berarti baris baru; Room mengisi id saat insert. */
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    /** Epoch millis transaksi; dasar urutan riwayat dan filter tanggal (batas
     *  hari dihitung dari zona waktu lokal, bukan UTC). */
    val dateMillis: Long = 0,
    /** Omzet kotor transaksi; sudah termasuk bagian yang dibon. */
    val total: Long = 0,
    /** Total margin dari selisih harga jual-modal saat transaksi dicatat. */
    val profit: Long = 0,
    /** Salah satu nilai [PayMethod]. Transaksi bersisa bayar selalu disimpan
     *  sebagai [PayMethod.BON], walau asalnya transfer. */
    val payMethod: String = PayMethod.TUNAI,
    /** Yang benar-benar diterima saat itu; untuk status LUNAS selalu = total. */
    val paidAmount: Long = 0,
    // Uang yang diserahkan pembeli (untuk hitung kembalian).
    // Null pada struk lama versi database 1.
    val tendered: Long? = null,
    /** Nama pembeli; kosong untuk penjualan tunai anonim. Dipakai untuk
     *  mencocokkan struk dengan bon pelanggan. */
    val customerName: String = "",
    /** [TxnStatus.LUNAS] atau [TxnStatus.BON]; BON otomatis membuat baris di
     *  tabel debts. */
    val status: String = TxnStatus.LUNAS,
    /** Catatan bebas kasir; hanya ditampilkan, tidak ikut dihitung. */
    val note: String = "",
)

/** Baris rincian struk. Nama, kategori, dan harga DISALIN dari produk saat
 *  checkout, jadi mengubah atau menghapus produk tidak mengubah struk lama. */
@Immutable
@Entity(
    tableName = Tables.SALE_ITEMS,
    indices = [
        Index("saleId"),
        Index("category"),
    ],
)
data class SaleItemEntity(
    /** 0 berarti baris baru; Room mengisi id saat insert batch. */
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    /** FK ke [Tables.SALES]; relasi tidak dipaksakan constraint database. */
    val saleId: Long = 0,
    /** Referensi lunak ke [Tables.PRODUCTS]; produknya boleh sudah dihapus. */
    val productId: Long = 0,
    /** Salinan nama produk saat transaksi. */
    val productName: String = "",
    /** Salinan kategori saat transaksi. `''` menandai baris dari database versi
     *  sebelum kategori ada, dan baris itu sengaja disaring dari filter kategori. */
    val category: String = "",
    /** Jumlah terjual; checkout memvalidasi nilainya > 0. */
    val qty: Int = 0,
    /** Salinan harga modal saat transaksi. */
    val buyPrice: Long = 0,
    /** Salinan harga jual saat transaksi. */
    val sellPrice: Long = 0,
    /** = qty * sellPrice pada saat transaksi. */
    val subtotal: Long = 0,
    /** = (sellPrice - buyPrice) * qty pada saat transaksi. */
    val profit: Long = 0,
)

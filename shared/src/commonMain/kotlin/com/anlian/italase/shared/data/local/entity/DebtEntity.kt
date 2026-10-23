package com.anlian.italase.shared.data.local.entity

import androidx.compose.runtime.Immutable
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.anlian.italase.shared.data.local.Tables

/** Status bon di kolom debts.status. Nilainya diturunkan dari remaining, tapi
 *  tetap disimpan supaya daftar bon bisa difilter lewat SQL. */
object DebtStatus {
    /** Masih ada sisa tagihan. */
    const val BELUM_LUNAS: String = "BELUM_LUNAS"

    /** Sisa sudah nol. */
    const val LUNAS: String = "LUNAS"
}

/** Piutang pelanggan (bon). Dibuat otomatis oleh checkout yang bersisa bayar
 *  atau manual dari layar Bon; satu baris = satu bon. paidAmount dan remaining
 *  adalah ringkasan yang harus tetap konsisten dengan debt_payments. */
@Immutable
@Entity(
    tableName = Tables.DEBTS,
    indices = [
        Index("status"),
        Index("dateMillis"),
    ],
)
data class DebtEntity(
    /** 0 berarti baris baru; Room mengisi id saat insert. */
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    /** Nama pelanggan; bon hasil checkout tanpa nama diisi ShopDefaults.CUSTOMER
     *  supaya selalu ada teks untuk ditampilkan di daftar. */
    val customerName: String = "",
    /** Nomor telepon opsional; hanya untuk pengingat, tidak dipakai mencari bon. */
    val phone: String = "",
    /** Struk asal bon; null untuk bon manual yang tidak terkait penjualan. */
    val saleId: Long? = null,
    /** Nilai bon saat dibuat; tidak ikut berubah kalau produknya diedit. */
    val totalAmount: Long = 0,
    /** Akumulasi pembayaran yang tercatat di debt_payments. */
    val paidAmount: Long = 0,
    /** = totalAmount - paidAmount dan tidak pernah negatif, karena kelebihan
     *  bayar dipotong ke sisa (lihat ShopRepository.payDebt). */
    val remaining: Long = 0,
    /** Turunan dari remaining; status LUNAS diset saat sisanya habis. */
    val status: String = DebtStatus.BELUM_LUNAS,
    /** Epoch millis bon dibuat; dasar urutan daftar bon. */
    val dateMillis: Long = 0,
    /** Catatan bebas, mis. janji bayar pelanggan. */
    val note: String = "",
)

/** Satu baris per cicilan bon. Tabel ini sumber kebenaran riwayat pembayaran,
 *  sedangkan debts.paidAmount hanya ringkasannya. */
@Immutable
@Entity(
    tableName = Tables.DEBT_PAYMENTS,
    indices = [
        Index("debtId"),
    ],
)
data class DebtPaymentEntity(
    /** 0 berarti baris baru; Room mengisi id saat insert. */
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    /** FK ke [Tables.DEBTS]. */
    val debtId: Long = 0,
    /** Nominal cicilan yang benar-benar tercatat; kelebihan dari sisa bon
     *  dipotong, bukan disimpan sebagai nilai negatif. */
    val amount: Long = 0,
    /** Epoch millis pembayaran diterima. */
    val dateMillis: Long = 0,
    /** Catatan bebas per cicilan. */
    val note: String = "",
)

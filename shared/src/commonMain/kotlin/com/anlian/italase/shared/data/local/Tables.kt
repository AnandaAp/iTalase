package com.anlian.italase.shared.data.local

/**
 * Nama tabel SQLite, satu sumber untuk semua `@Entity` dan `@Query`.
 * Query DAO ditulis sebagai SQL mentah dengan interpolasi konstanta ini,
 * jadi typo nama tabel tidak ketahuan saat compile melainkan baru crash saat
 * runtime — jangan tulis literal nama tabel di luar file ini.
 */
object Tables {
    /** Master barang; satu baris per SKU, termasuk yang stoknya nol. */
    const val PRODUCTS = "products"

    /** Header struk: satu baris per transaksi kasir. */
    const val SALES = "sales"

    /** Rincian baris struk; nama dan harga disalin saat transaksi berjalan. */
    const val SALE_ITEMS = "sale_items"

    /** Piutang pelanggan (bon), baik yang masih aktif maupun sudah lunas. */
    const val DEBTS = "debts"

    /** Riwayat cicilan bon, satu baris per pembayaran. */
    const val DEBT_PAYMENTS = "debt_payments"

    /** Pengeluaran operasional toko, di luar pembelian stok. */
    const val EXPENSES = "expenses"
}

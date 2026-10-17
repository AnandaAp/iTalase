package com.anlian.italase.shared.util

import com.anlian.italase.shared.data.local.entity.ProductEntity

/**
 * Daftar produk yang sudah **terurut** dan sudah punya kunci huruf-kecil.
 *
 * ## Kenapa kelas ini ada (algoritma load data → layar)
 *
 * Pipeline lama: `combine(products, query, category) { filterProducts(...) }`.
 * Artinya setiap ketikan user mengulang SELURUH pekerjaan atas seluruh tabel:
 * `lowercase()` 3x per produk, lalu `sortedWith` atas n baris. Padahal yang
 * berubah saat mengetik cuma query-nya — daftar produknya tidak.
 *
 * Di sini pekerjaan itu dipisah sesuai apa yang sebenarnya berubah:
 *
 * | berubah | dikerjakan ulang |
 * |---|---|
 * | tabel produk (1x per transaksi) | urutkan + hitung kunci huruf-kecil |
 * | query / kategori (per ketikan) | `contains` saja, tanpa alokasi string |
 *
 * Jadi biaya per ketikan turun dari O(n log n) + 3n alokasi string menjadi O(n)
 * perbandingan. Layar tidak perlu tahu soal ini; ia hanya menerima hasil saring.
 *
 * `ponytail:` daftar penuh tetap tinggal di memori, sama seperti sebelumnya —
 * pada 5.000 SKU itu masih di bawah 1 MB dan di bawah 5 ms per saring. Kalau
 * suatu hari penjual melewati ~20.000 SKU, naikkan ke saring-di-SQL
 * (`@RawQuery` + `LIMIT`) supaya Room berhenti mengirim seluruh tabel; jangan
 * tambah indeks dulu — yang mahal itu transfer barisnya, bukan pencariannya.
 */
class ProductList private constructor(private val rows: List<Row>) {

    /** Satu baris + kunci pencarian yang sudah dihuruf-kecilkan sekali saja. */
    private class Row(
        val product: ProductEntity,
        val name: String,
        val barcode: String,
        val category: String,
    ) {
        /**
         * Cocok kalau SEMUA kata kunci ketemu di nama, barcode, atau kategori (ketiganya sudah
         * huruf kecil).
         *
         * Urutan kata tidak penting, jadi "goreng indomie" tetap menemukan "Indomie Goreng".
         */
        fun matches(tokens: List<String>): Boolean =
            tokens.all { t -> name.contains(t) || barcode.contains(t) || category.contains(t) }
    }

    /**
     * Saring daftar yang SUDAH terurut — tidak menyortir ulang, dan urutan
     * hasilnya sama dengan urutan sumber (syarat "stok habis di bawah" tetap).
     *
     * Aturan kata kunci (tidak berubah dari versi lama): query dipecah per
     * spasi, SEMUA kata wajib cocok, tiap kata boleh cocok di nama / barcode /
     * kategori, tak peduli huruf besar-kecil.
     */
    fun visible(query: String, category: String?): List<ProductEntity> {
        val wanted: String? = category?.takeIf { it.isNotBlank() }
        val tokens: List<String> = query.trim().lowercase()
            .split(TOKEN_SEPARATOR)
            .filter { it.isNotEmpty() }

        // Kasus paling sering (Kasir/Stok dibuka tanpa kata kunci): tidak ada
        // yang perlu disaring, dan daftarnya sudah terurut dari [of].
        if (wanted == null && tokens.isEmpty()) return rows.map { it.product }

        return rows.asSequence()
            .filter { r ->
                (wanted == null || r.product.category == wanted) && r.matches(tokens)
            }
            .map { it.product }
            .toList()
    }

    companion object {
        /**
         * Daftar kosong, dipakai sebagai nilai awal `stateIn` sebelum tabel selesai dibaca.
         */
        val EMPTY: ProductList = ProductList(emptyList())

        /**
         * Satu atau lebih spasi/putih — query "susu  kental" berarti dua kata kunci.
         */
        private val TOKEN_SEPARATOR = Regex("\\s+")

        /**
         * Urutan tampil (dipakai Kasir DAN Stok):
         *   1. yang masih ada stok di atas — yang habis (stock <= 0) di paling bawah
         *   2. di dalam tiap kelompok, urut abjad nama (case-insensitive)
         *
         * `stock <= 0` dipakai sebagai kunci Boolean: `false` (= masih ada)
         * mendahului `true` karena Boolean Comparable.
         */
        fun of(products: List<ProductEntity>): ProductList = ProductList(
            products
                .map {
                    Row(
                        product = it,
                        name = it.name.lowercase(),
                        barcode = it.barcode.lowercase(),
                        category = it.category.lowercase(),
                    )
                }
                .sortedWith(compareBy({ it.product.stock <= 0 }, { it.name })),
        )
    }
}

package com.anlian.italase.shared.data

/** Nilai default untuk field yang sengaja dibiarkan kosong oleh kasir, supaya
 *  kolom wajib tetap terisi teks yang masuk akal di struk dan laporan. */
object ShopDefaults {
    /** Satuan barang baru sebelum diubah manual di layar produk. */
    const val UNIT = "pcs"

    /** Kategori pengeluaran baru; hanya label, tidak memengaruhi hitungan. */
    const val CATEGORY = "Operasional"

    /** Nama pengganti saat checkout bon tanpa nama pelanggan, agar bon tetap
     *  bisa dicari dan tidak tercampur dengan bon pelanggan lain yang bernama sama. */
    const val CUSTOMER = "Pelanggan"
}

/** Ambang stok yang bukan milik barang tertentu, jadi tidak bisa diatur pengguna. */
object StockRules {
    /** Batas cadangan: barang dianggap menipis kalau stock <= nilai ini,
     *  bahkan ketika minStock-nya masih 0 (barang yang belum pernah diatur).
     *  Tanpa ini, stok 0 pun tidak akan pernah muncul di daftar stok menipis. */
    const val LOW_STOCK_FALLBACK = 3
}

/** Kategori bawaan produk. Nilai string-nya ikut tersimpan di kolom
 *  `product.category`, jadi MENGGANTI teks di sini membuat baris lama jadi
 *  kategori tak dikenal — bukan rename kategori. */
object ProductCategories {
    /** Makanan siap saji dan camilan. */
    const val FOOD = "Makanan"

    /** Minuman, termasuk kemasan. */
    const val DRINK = "Minuman"

    /** Sembako: beras, gula, minyak, dan sejenisnya. */
    const val STAPLE = "Sembako"

    /** Perlengkapan rumah tangga: sabun, pembersih, alat. */
    const val HOUSEHOLD = "Rumah Tangga"

    /** Kesehatan: obat bebas, masker, vitamin. */
    const val HEALTH = "Kesehatan"

    /** Muara untuk barang yang tidak pas di kategori lain; juga default produk baru. */
    const val OTHER = "Lainnya"

    /** Urutan tetap untuk dropdown kategori di layar produk dan filter riwayat. */
    val ALL: List<String> = listOf(FOOD, DRINK, STAPLE, HOUSEHOLD, HEALTH, OTHER)
}

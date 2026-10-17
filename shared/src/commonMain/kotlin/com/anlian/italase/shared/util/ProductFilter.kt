package com.anlian.italase.shared.util

import com.anlian.italase.shared.data.local.entity.ProductEntity

// Pencarian warung: query dipecah per kata, semua kata wajib cocok di
// nama / barcode / kategori (tak peduli huruf besar-kecil dan spasi ganda),
// plus saringan kategori. Murni agar mudah diuji.
//
// URUTAN TAMPIL (dipakai Kasir DAN Stok):
//   1. yang masih ada stok di atas — yang habis (stock <= 0) di paling bawah
//   2. di dalam tiap kelompok, urut abjad nama (case-insensitive)
// Ditaruh di sini, bukan di SQL, karena fungsi ini satu-satunya choke point
// untuk kedua layar dan fungsi murni inilah yang punya test. `ORDER BY name`
// di DAO tidak cukup: ia tidak tahu soal stok habis.
fun filterProducts(
    products: List<ProductEntity>,
    query: String,
    category: String?,
): List<ProductEntity> {
    val tokens: List<String> = query.trim().lowercase()
        .split(Regex("\\s+"))
        .filter { it.isNotEmpty() }
    return products
        .filter { p ->
            (category.isNullOrBlank() || p.category == category) &&
                tokens.all { t ->
                    p.name.lowercase().contains(t) ||
                        p.barcode.lowercase().contains(t) ||
                        p.category.lowercase().contains(t)
                }
        }
        // Boolean ikut Comparable: false (= masih ada stok) mendahului true.
        .sortedWith(
            compareBy<ProductEntity>(
                { it.stock <= 0 },
                { it.name.lowercase() },
            ),
        )
}

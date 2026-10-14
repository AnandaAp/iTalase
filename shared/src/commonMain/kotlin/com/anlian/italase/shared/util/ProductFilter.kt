package com.anlian.italase.shared.util

import com.anlian.italase.shared.data.local.entity.ProductEntity

// Pencarian warung: query dipecah per kata, semua kata wajib cocok di
// nama / barcode / kategori (tak peduli huruf besar-kecil dan spasi ganda),
// plus saringan kategori. Murni agar mudah diuji.
fun filterProducts(
    products: List<ProductEntity>,
    query: String,
    category: String?,
): List<ProductEntity> {
    val tokens: List<String> = query.trim().lowercase()
        .split(Regex("\\s+"))
        .filter { it.isNotEmpty() }
    return products.filter { p ->
        (category.isNullOrBlank() || p.category == category) &&
            tokens.all { t ->
                p.name.lowercase().contains(t) ||
                    p.barcode.lowercase().contains(t) ||
                    p.category.lowercase().contains(t)
            }
    }
}

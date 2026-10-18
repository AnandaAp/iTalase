package com.anlian.italase.shared.util

/**
 * Format rupiah gaya Indonesia: titik sebagai pemisah ribuan, tanpa desimal, negatif
 * ditulis `-Rp…`.
 *
 * Ditulis tangan karena dipakai juga di `commonMain` (di luar jangkauan `NumberFormat`), dan
 * satu format tetap inilah yang dilihat penjual di seluruh aplikasi. Nol jadi kasus khusus
 * karena loop di bawah tidak pernah masuk untuk nilai 0.
 */
fun formatRupiah(amount: Long): String {
    if (amount == 0L) return "Rp0"
    val neg: Boolean = amount < 0
    var n: Long = if (neg) -amount else amount
    val parts: MutableList<String> = mutableListOf()
    while (n > 0) {
        val rem: Long = n % 1000
        n /= 1000
        if (n > 0) parts.add(rem.toString().padStart(3, '0'))
        else parts.add(rem.toString())
    }
    val joined: String = parts.reversed().joinToString(".")
    return (if (neg) "-Rp" else "Rp") + joined
}

/**
 * Kebalikan [formatRupiah]: ambil digitnya saja.
 *
 * Teks tanpa digit dan angka yang melewati jangkauan `Long` sama-sama jadi 0 — pemanggil
 * tidak pernah perlu membedakan keduanya.
 */
fun parseRupiah(text: String): Long {
    val digits: String = text.filter { it.isDigit() }
    if (digits.isEmpty()) return 0L
    return digits.toLongOrNull() ?: 0L
}

/**
 * Satu hari dalam milidetik (24 jam tetap); dipakai untuk menghitung batas hari, bukan
 * aritmetika kalender.
 */
const val DAY_MILLIS = 86_400_000L

// Waktu, tanggal, dan format tanggal Indonesia ada di Time.kt — satu
// implementasi commonMain lewat kotlinx-datetime (dulu expect/actual per platform).

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

/**
 * Waktu sekarang sebagai epoch millis.
 *
 * Pembungkus tipis [currentTimeMillis] supaya kode `commonMain` tidak menyentuh API platform
 * secara langsung.
 */
fun nowMillis(): Long = currentTimeMillis()

/**
 * Jam sistem per platform; satu-satunya tempat waktu dibaca langsung.
 */
expect fun currentTimeMillis(): Long

// Batas hari menurut zona waktu perangkat (bukan UTC), agar
// "hari ini" benar untuk warung di WIB/WITA/WIT.
/**
 * Awal hari menurut zona waktu **perangkat**, bukan UTC.
 *
 * Zona lokal dipakai karena batas "hari ini" di laporan harus sama dengan hari yang dialami
 * penjual; perangkat di Indonesia selalu berada di WIB/WITA/WIT.
 */
expect fun startOfLocalDayMillis(millis: Long): Long

/**
 * Akhir hari = awal hari berikutnya dikurangi 1 ms.
 *
 * Rentang `[awal, akhir]` jadi tertutup penuh: tidak ada milidetik yang jatuh di dua hari,
 * dan tidak ada celah yang bisa membuat transaksi terlewat dari laporan.
 */
fun endOfLocalDayMillis(millis: Long): Long =
    startOfLocalDayMillis(millis + DAY_MILLIS) - 1

// Contoh: "14 Nov 2023 • 09:41" sesuai locale Indonesia.
/**
 * Tanggal + jam lokal, contoh "14 Nov 2023 • 09:41" sesuai locale Indonesia.
 */
expect fun formatDateTime(millis: Long): String

package com.anlian.italase.shared.util

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

fun parseRupiah(text: String): Long {
    val digits: String = text.filter { it.isDigit() }
    if (digits.isEmpty()) return 0L
    return digits.toLongOrNull() ?: 0L
}

const val DAY_MILLIS = 86_400_000L

fun nowMillis(): Long = currentTimeMillis()

expect fun currentTimeMillis(): Long

// Batas hari menurut zona waktu perangkat (bukan UTC), agar
// "hari ini" benar untuk warung di WIB/WITA/WIT.
expect fun startOfLocalDayMillis(millis: Long): Long

fun endOfLocalDayMillis(millis: Long): Long =
    startOfLocalDayMillis(millis + DAY_MILLIS) - 1

// Contoh: "14 Nov 2023 • 09:41" sesuai locale Indonesia.
expect fun formatDateTime(millis: Long): String

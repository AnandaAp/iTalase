package com.anlian.italase.shared.util

import android.os.Build

/**
 * Jam dinding perangkat; cukup untuk stempel waktu transaksi.
 */
actual fun currentTimeMillis(): Long = System.currentTimeMillis()

/**
 * Awal hari menurut zona perangkat.
 *
 * Dihitung lewat `java.time` supaya zona dan pergeseran jam (DST) ditangani pustaka, bukan
 * kode kita.
 */
actual fun startOfLocalDayMillis(millis: Long): Long {
    val zone: java.time.ZoneId = java.time.ZoneId.systemDefault()
    return java.time.Instant.ofEpochMilli(millis)
        .atZone(zone)
        .toLocalDate()
        .atStartOfDay(zone)
        .toInstant()
        .toEpochMilli()
}

/**
 * Formatter bersama, bukan dibuat per panggilan: `ofPattern` mahal sedangkan riwayat
 * memformat satu tanggal untuk setiap baris.
 *
 * Cabang BAKLAVA memakai `Locale.of` karena konstruktor `Locale(String, String)` sudah tidak
 * tersedia di API tersebut; keduanya menghasilkan pola dan locale yang sama.
 */
private val dateTimeFormatter: java.time.format.DateTimeFormatter = when {
    Build.VERSION.SDK_INT >= Build.VERSION_CODES.BAKLAVA -> java.time.format.DateTimeFormatter.ofPattern(
        "d MMM yyyy • HH:mm",
        java.util.Locale.of("id", "ID")
    )
    else -> java.time.format.DateTimeFormatter.ofPattern(
        "d MMM yyyy • HH:mm",
        java.util.Locale("id", "ID")
    )
}

/**
 * Tanggal + jam lokal ("14 Nov 2023 • 09:41") sesuai locale Indonesia.
 */
actual fun formatDateTime(millis: Long): String {
    val zone: java.time.ZoneId = java.time.ZoneId.systemDefault()
    return java.time.Instant.ofEpochMilli(millis).atZone(zone).format(dateTimeFormatter)
}

package com.anlian.italase.shared.util

import android.os.Build

actual fun currentTimeMillis(): Long = System.currentTimeMillis()

actual fun startOfLocalDayMillis(millis: Long): Long {
    val zone: java.time.ZoneId = java.time.ZoneId.systemDefault()
    return java.time.Instant.ofEpochMilli(millis)
        .atZone(zone)
        .toLocalDate()
        .atStartOfDay(zone)
        .toInstant()
        .toEpochMilli()
}

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

actual fun formatDateTime(millis: Long): String {
    val zone: java.time.ZoneId = java.time.ZoneId.systemDefault()
    return java.time.Instant.ofEpochMilli(millis).atZone(zone).format(dateTimeFormatter)
}

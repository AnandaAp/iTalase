package com.anlian.italase.shared.util

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class FormatTest {
    @Test
    fun rupiahZero() = assertEquals("Rp0", formatRupiah(0))

    @Test
    fun rupiahThousand() = assertEquals("Rp15.000", formatRupiah(15_000))

    @Test
    fun rupiahMillion() = assertEquals("Rp1.250.000", formatRupiah(1_250_000))

    @Test
    fun rupiahNegative() = assertEquals("-Rp32.000", formatRupiah(-32_000))

    @Test
    fun parseDigits() = assertEquals(15000L, parseRupiah("Rp15.000"))

    @Test
    fun parseOverflowFallsBackToZero() = assertEquals(0L, parseRupiah("Rp99999999999999999999999"))

    @Test
    fun localDayBounds() {
        val now: Long = 1_700_000_000_000L
        val s: Long = startOfLocalDayMillis(now)
        val e: Long = endOfLocalDayMillis(now)
        assertTrue(now in s..e, "now must fall inside its local day")
        // Sehari penuh 24 jam; toleransi DST untuk zona yang menerapkannya.
        assertTrue(e - s in 82_800_000L..90_000_000L, "day span out of range: ${e - s}")
    }
}

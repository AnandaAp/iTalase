package com.anlian.italase.shared.ui.components

import com.anlian.italase.shared.util.dayRangeMillis
import com.anlian.italase.shared.util.daysBetweenInclusive
import com.anlian.italase.shared.util.endOfDayMillis
import com.anlian.italase.shared.util.formatDateLong
import com.anlian.italase.shared.util.formatDateRange
import com.anlian.italase.shared.util.minusDays
import com.anlian.italase.shared.util.shiftDays
import com.anlian.italase.shared.util.startOfDayMillis
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Unit test logika perhitungan dan preset DateRangePicker (AUD-100 & CARD-103).
 * Menguji kalkulasi rentang cepat (7, 30, 90 hari), jendela gulir 120 hari,
 * transisi pergantian bulan/tahun/kabisat, dan konsistensi epoch batas hari.
 */
class DateRangePickerLogicTest {

    private val presetDays = listOf(7, 30, 90)

    @Test
    fun testPresetRangeCalculationsMatchExactDayCounts() {
        val today = LocalDate(2026, 10, 29)

        presetDays.forEach { days ->
            val start = today.minusDays(days - 1)
            val end = today

            val count = daysBetweenInclusive(start, end)
            assertEquals(days, count, "Preset $days hari harus menghasilkan rentang tepat $days hari inklusif")

            val (startMillis, endMillis) = dayRangeMillis(start, end)
            assertTrue(endMillis > startMillis, "Epoch akhir harus lebih besar dari epoch awal")
            val diffDays = (endMillis - startMillis + 1) / (24 * 60 * 60 * 1000L)
            assertEquals(days.toLong(), diffDays, "Durasi milidetik harus setara dengan $days hari penuh")
        }
    }

    @Test
    fun testRolling120DaysListProperties() {
        val today = LocalDate(2026, 10, 29)
        val dates: List<LocalDate> = (0 until 120).map { today.minusDays(it) }

        assertEquals(120, dates.size, "Jendela pemilih tanggal harus memiliki tepat 120 entri")
        assertEquals(today, dates.first(), "Entri pertama harus hari ini")
        assertEquals(today.minusDays(119), dates.last(), "Entri terakhir harus hari ini minus 119 hari")

        // Verifikasi urutan strictly descending
        for (i in 0 until dates.size - 1) {
            assertTrue(dates[i] > dates[i + 1], "Daftar tanggal harus berurutan menurun")
        }
    }

    @Test
    fun testMonthAndYearBoundaryTransitions() {
        // Transisi awal tahun: 1 Januari 2026 minus 1 hari = 31 Desember 2025
        val newYear = LocalDate(2026, 1, 1)
        val yearEve = newYear.minusDays(1)
        assertEquals(LocalDate(2025, 12, 31), yearEve)

        // Transisi pergantian bulan non-kabisat: 1 Maret 2026 minus 1 hari = 28 Februari 2026
        val marchFirst2026 = LocalDate(2026, 3, 1)
        val febEnd2026 = marchFirst2026.minusDays(1)
        assertEquals(LocalDate(2026, 2, 28), febEnd2026)

        // Transisi pergantian bulan tahun kabisat: 1 Maret 2028 minus 1 hari = 29 Februari 2028
        val marchFirst2028 = LocalDate(2028, 3, 1)
        val febEnd2028 = marchFirst2028.minusDays(1)
        assertEquals(LocalDate(2028, 2, 29), febEnd2028)
    }

    @Test
    fun testDateRangeFormattingVariations() {
        val today = LocalDate(2026, 10, 29)
        val startOfSameMonth = LocalDate(2026, 10, 1)
        val startOfPrevMonth = LocalDate(2026, 9, 1)

        // Rentang dalam bulan yang sama
        val sameMonthLabel = formatDateRange(startOfSameMonth, today)
        assertEquals("1 - 29 Oktober 2026", sameMonthLabel)

        // Rentang lintas bulan
        val crossMonthLabel = formatDateRange(startOfPrevMonth, today)
        assertTrue(crossMonthLabel.contains("September") && crossMonthLabel.contains("Oktober"))

        // Single date
        val singleDateLabel = formatDateRange(today, today)
        assertEquals("29 Oktober 2026", singleDateLabel)

        // Tanpa filter (semua tanggal)
        assertEquals("Semua tanggal", formatDateRange(null, null))
    }

    @Test
    fun testDayRangeEpochBoundariesSafety() {
        val d = LocalDate(2026, 10, 29)
        val start = startOfDayMillis(d)
        val end = endOfDayMillis(d)

        assertNotNull(start)
        assertNotNull(end)
        assertTrue(end > start)
        assertEquals(86_399_999L, end - start, "Selisih awal dan akhir hari harus 23:59:59.999")

        // Verifikasi format tanggal panjang
        val formatted = formatDateLong(start)
        assertTrue(formatted.contains("29 Oktober 2026"))
    }
}

package com.anlian.italase.shared.util

import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Unit test komprehensif untuk modul utilitas waktu dan rentang tanggal (Time.kt).
 * Memverifikasi batas waktu lokal, penamaan bulan Indonesia, pergeseran hari,
 * serta formatting rentang tanggal yang digunakan oleh DateRangePicker & PDF Reporting.
 */
class TimeTest {

    @Test
    fun testCurrentTimeAndNowMillis() {
        val now1 = currentTimeMillis()
        val now2 = nowMillis()
        assertTrue(now1 > 0)
        assertTrue(now2 >= now1)
    }

    @Test
    fun testLocalDateAndDateTimeConversion() {
        // 17 Agustus 2026 09:30 UTC = 1786959000000L
        val millis = 1786959000000L
        val dateTime = localDateTimeOf(millis)
        val date = localDateOf(millis)

        assertEquals(date, dateTime.date)
        assertTrue(date.year >= 2026)
    }

    @Test
    fun testStartAndEndOfLocalDayMillis() {
        val now = nowMillis()
        val start = startOfLocalDayMillis(now)
        val end = endOfLocalDayMillis(now)

        assertTrue(now in start..end, "nowMillis must fall within start and end of day")
        assertTrue(end > start, "end of day must be strictly after start of day")
        // Selisih akhir dan awal hari harus 24 jam kurang 1 ms (86399999 ms) toleransi DST
        val diff = end - start
        assertTrue(diff in 82_799_999L..90_000_000L, "Day duration should be ~24h")
    }

    @Test
    fun testStartAndEndOfDayMillisForLocalDate() {
        val date = LocalDate(2026, 8, 17)
        val start = startOfDayMillis(date)
        val end = endOfDayMillis(date)

        assertNotNull(start)
        assertNotNull(end)
        assertTrue(end > start)
        assertEquals(86_399_999L, end - start)

        assertNull(startOfDayMillis(null))
        assertNull(endOfDayMillis(null))
    }

    @Test
    fun testDayRangeMillis() {
        val d1 = LocalDate(2026, 8, 1)
        val d2 = LocalDate(2026, 8, 31)

        val (from, to) = dayRangeMillis(d1, d2)
        assertEquals(startOfDayMillis(d1), from)
        assertEquals(endOfDayMillis(d2), to)

        val (openFrom, openTo) = dayRangeMillis(null, null)
        assertEquals(Long.MIN_VALUE, openFrom)
        assertEquals(Long.MAX_VALUE, openTo)
    }

    @Test
    fun testDateFormattingIndonesianNames() {
        val date = LocalDate(2026, 8, 17)
        val millis = startOfDayMillis(date)!!

        val dateLong = formatDateLong(millis)
        assertTrue(dateLong.contains("17"))
        assertTrue(dateLong.contains("Agustus"))
        assertTrue(dateLong.contains("2026"))

        val dateShort = formatDateShort(millis)
        assertTrue(dateShort.contains("Agu"))

        val withDay = formatDateTimeWithDay(millis)
        assertTrue(withDay.contains("Senin") || withDay.contains("Agustus"))
    }

    @Test
    fun testFormatDateRangeVariations() {
        // 1. Both null
        assertEquals("Semua tanggal", formatDateRange(null, null))

        // 2. From only
        val d1 = LocalDate(2026, 8, 1)
        val fromOnly = formatDateRange(d1, null)
        assertTrue(fromOnly.startsWith("Sejak"))
        assertTrue(fromOnly.contains("Agustus 2026"))

        // 3. To only
        val toOnly = formatDateRange(null, d1)
        assertTrue(toOnly.startsWith("Sampai"))
        assertTrue(toOnly.contains("Agustus 2026"))

        // 4. Same day
        val sameDay = formatDateRange(d1, d1)
        assertEquals(formatDateLong(startOfDayMillis(d1)!!), sameDay)

        // 5. Same month and year
        val d2 = LocalDate(2026, 8, 17)
        val sameMonth = formatDateRange(d1, d2)
        assertEquals("1 - 17 Agustus 2026", sameMonth)

        // 6. Cross month
        val d3 = LocalDate(2026, 9, 5)
        val crossMonth = formatDateRange(d1, d3)
        assertTrue(crossMonth.contains("Agustus 2026"))
        assertTrue(crossMonth.contains("September 2026"))
    }

    @Test
    fun testDaysBetweenInclusive() {
        assertNull(daysBetweenInclusive(null, null))
        assertNull(daysBetweenInclusive(LocalDate(2026, 8, 1), null))
        assertNull(daysBetweenInclusive(null, LocalDate(2026, 8, 1)))

        val d1 = LocalDate(2026, 8, 1)
        val d2 = LocalDate(2026, 8, 7)
        assertEquals(1, daysBetweenInclusive(d1, d1))
        assertEquals(7, daysBetweenInclusive(d1, d2))
    }

    @Test
    fun testShiftAndMinusDays() {
        val today = LocalDate(2026, 8, 10)
        val minus7 = today.minusDays(7)
        val plus7 = today.shiftDays(7)

        assertEquals(LocalDate(2026, 8, 3), minus7)
        assertEquals(LocalDate(2026, 8, 17), plus7)
    }
}

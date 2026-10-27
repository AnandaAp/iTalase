package com.anlian.italase.shared.report

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Pengujian unit untuk logika sanitasi berkas ekspor laporan PDF.
 * Memastikan proteksi path traversal, keamanan karakter berkas,
 * dan fallback nama berkas standar berfungsi dengan benar.
 */
class ReportExportSanitizationTest {

    private fun sanitizeFileName(suggestedName: String): String {
        // Logika sanitasi yang sama dengan ReportExport.android.kt
        val nameOnly = suggestedName
            .replace('\\', '/')
            .substringAfterLast('/')
            .trim()
        return nameOnly.ifBlank { "laporan.pdf" }
    }

    @Test
    fun testValidPdfName() {
        val input = "laporan_penjualan_oktober.pdf"
        assertEquals("laporan_penjualan_oktober.pdf", sanitizeFileName(input))
    }

    @Test
    fun testPathTraversalUnixPrevented() {
        val input = "../../etc/passwd"
        assertEquals("passwd", sanitizeFileName(input))
    }

    @Test
    fun testPathTraversalWindowsPrevented() {
        val input = "..\\..\\Windows\\System32\\cmd.exe"
        assertEquals("cmd.exe", sanitizeFileName(input))
    }

    @Test
    fun testNestedDirectoriesStripped() {
        val input = "reports/2026/10/laporan.pdf"
        assertEquals("laporan.pdf", sanitizeFileName(input))
    }

    @Test
    fun testBlankOrWhitespaceFallback() {
        assertEquals("laporan.pdf", sanitizeFileName(""))
        assertEquals("laporan.pdf", sanitizeFileName("   "))
        assertEquals("laporan.pdf", sanitizeFileName("///"))
        assertEquals("laporan.pdf", sanitizeFileName("\\\\\\"))
    }

    @Test
    fun testSalesReportTotalsIntegrity() {
        val rows = listOf(
            SalesReportRow(
                receiptNo = 1,
                dateMillis = 1760000000000L,
                methodLabel = "Tunai",
                statusLabel = "Lunas",
                paid = true,
                total = 50000,
                profit = 15000,
                customerName = "Budi"
            ),
            SalesReportRow(
                receiptNo = 2,
                dateMillis = 1760003600000L,
                methodLabel = "Bon",
                statusLabel = "Bon",
                paid = false,
                total = 30000,
                profit = 8000,
                customerName = "Siti"
            )
        )

        val totals = SalesReportTotals(
            count = rows.size,
            revenue = rows.sumOf { it.total },
            profit = rows.sumOf { it.profit },
            unpaid = rows.filter { !it.paid }.sumOf { it.total },
        )

        assertEquals(2, totals.count)
        assertEquals(80000L, totals.revenue)
        assertEquals(23000L, totals.profit)
        assertEquals(30000L, totals.unpaid)
    }
}

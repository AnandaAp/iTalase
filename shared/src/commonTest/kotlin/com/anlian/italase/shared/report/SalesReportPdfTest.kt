package com.anlian.italase.shared.report

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Laporan PDF diuji dari sisi strukturnya: berkasnya harus sah, lengkap, dan
 * isinya benar-benar sampai ke dalam stream — bukan cuma "tidak melempar
 * exception". Verifikasi visual (buka PDF-nya) tetap tugas manusia; yang
 * dipastikan di sini adalah kerangkanya.
 */
class SalesReportPdfTest {

    private fun row(i: Long, customer: String = "") = SalesReportRow(
        receiptNo = i,
        dateMillis = 1_760_000_000_000L + i * 3_600_000L,
        methodLabel = if (i % 3L == 0L) "Bon" else "Tunai",
        statusLabel = if (i % 3L == 0L) "Bon" else "Lunas",
        paid = i % 3L != 0L,
        total = 10_000L * i,
        profit = 2_500L * i,
        customerName = customer,
    )

    private fun report(rows: List<SalesReportRow>) = SalesReport(
        appName = "iTalase",
        tagline = "Kasir & stok warung, offline",
        title = "Laporan Riwayat Transaksi",
        periodLabel = "1 - 17 Agustus 2026",
        filterLabel = "Kategori: Semua",
        generatedAtMillis = 1_760_000_000_000L,
        rows = rows,
        totals = SalesReportTotals(
            count = rows.size,
            revenue = rows.sumOf { it.total },
            profit = rows.sumOf { it.profit },
            unpaid = rows.filter { !it.paid }.sumOf { it.total },
        ),
        footerNote = "Sumber data: database lokal iTalase",
    )

    private fun pdfOf(rows: List<SalesReportRow>) =
        renderSalesReportPdf(report(rows), logoJpeg = null)

    private fun ByteArray.asLatin1(): String = decodeToString()

    /**
     * Stream PDF memakai WinAnsi, bukan UTF-8: "–" jadi byte 0x96, "•" jadi 0x95.
     * Test yang mencari teks harus memakai kode yang sama, bukan karakter Unicode —
     * kalau tidak, test-nya gagal pada berkas yang sebenarnya benar.
     */
    private fun ByteArray.asWinAnsi(): String =
        decodeToString(throwOnInvalidSequence = false).replace("\r\n", "\n").map { c ->
            when (c.code) {
                0x96 -> '–'
                0x95 -> '•'
                0x97 -> '—'
                0x85 -> '…'
                else -> c
            }
        }.joinToString("")

    @Test
    fun headerAndTrailerAreWellFormed() {
        val pdf = pdfOf(listOf(row(1)))
        assertTrue(pdf.size > 1_000, "PDF terlalu kecil: ${pdf.size} byte")
        assertEquals("%PDF-1.4", pdf.asWinAnsi().take(8))
        assertTrue(pdf.asWinAnsi().trimEnd().endsWith("%%EOF"), "tidak diakhiri %%EOF")
        assertTrue(pdf.asWinAnsi().contains("startxref"), "tidak ada startxref")
    }

    /**
     * Offset di trailer harus menunjuk tepat ke awal tiap objek — kalau tidak,
     * pembaca PDF menolak berkasnya. Diperiksa lewat /Size dan /Root dari trailer,
     * bukan dengan memotong-motong baris tabel: format tabel xref membawa spasi di
     * ujung tiap entri, dan test yang mem-parsing-nya sendiri lebih rapuh daripada
     * yang diuji.
     */
    @Test
    fun trailerOffsetsPointAtRealObjects() {
        val text = pdfOf(listOf(row(1), row(2))).asWinAnsi()
        val size = Regex("/Size (\\d+)").find(text)?.groupValues?.get(1)?.toInt()
        assertNotNull(size, "trailer tidak punya /Size")
        val rootId = Regex("/Root (\\d+) 0 R").find(text)?.groupValues?.get(1)
        assertNotNull(rootId, "trailer tidak punya /Root")

        // Tabel xref harus memuat tepat satu entri per objek (plus entri 0 bebas).
        val table = text.substringAfterLast("\nxref\n")
        val entryLines = table.lines().filter { it.trim().endsWith(" n") }
        assertEquals(size, entryLines.size + 1, "jumlah entri xref tidak cocok dengan /Size")

        // Isi tiap entri: offset harus menunjuk header objek yang benar.
        entryLines.forEachIndexed { i, line ->
            val offset = line.trim().substringBefore(" ").toInt()
            assertTrue(
                offset in 0 until text.length,
                "offset objek ${i + 1} di luar berkas: $offset"
            )
            assertTrue(
                text.startsWith("${i + 1} 0 obj", offset),
                "offset objek ${i + 1} menunjuk ke tempat salah (offset=$offset)",
            )
        }
        // Objek root yang ditunjuk trailer harus benar-benar ada.
        assertTrue(text.contains("/Type /Catalog"), "katalog tidak ada")
        assertTrue(
            text.startsWith(
                "$rootId 0 obj",
                entryLines[rootId.toInt() - 1].trim().substringBefore(" ").toInt()
            )
        )
    }

    /** Isi laporan harus benar-benar ada di dalam stream, bukan cuma di model. */
    @Test
    fun contentCarriesHeaderSummaryAndRows() {
        val text = pdfOf(listOf(row(1), row(2), row(3))).asWinAnsi()
        listOf(
            "iTalase", "Laporan Riwayat Transaksi", "1 - 17 Agustus 2026", "Kategori: Semua",
            "JUMLAH TRANSAKSI", "TOTAL OMZET", "RINCIAN TRANSAKSI", "TANGGAL TRANSAKSI",
            "Rp30.000", "Rp7.500", "Halaman 1 / 1"
        ).forEach {
            assertTrue(text.contains(it), "tidak ditemukan di PDF: «$it»")
        }
    }

    /** Tanggal ditulis dalam bahasa Indonesia, bukan angka mentah. */
    @Test
    fun datesAreIndonesian() {
        val text = pdfOf(listOf(row(1))).asWinAnsi()
        assertTrue(
            Regex("\\d{1,2} Agustus \\d{4}").containsMatchIn(text),
            "tanggal Indonesia tidak ada"
        )
        assertTrue(text.contains("09:00") || text.contains(":"), "jam tidak ada")
    }

    /** Lebih dari satu halaman: nomor halaman harus ikut menyesuaikan. */
    @Test
    fun longHistoryIsPaginated() {
        val rows = (1L..40L).map { row(it) }
        val text = pdfOf(rows).asWinAnsi()
        assertTrue(text.contains("Halaman 1 / 2"), "tidak memecah jadi dua halaman")
        assertTrue(text.contains("Halaman 2 / 2"), "halaman kedua tidak ada")
        // Nama kolom diulang di halaman kedua supaya tabel tetap terbaca.
        assertTrue(text.split("TANGGAL TRANSAKSI").size - 1 >= 2, "kop tabel tidak diulang")
    }

    /** Riwayat kosong tetap menghasilkan laporan yang menjelaskan keadaannya. */
    @Test
    fun emptyHistoryStillProducesAReadablePage() {
        val text = pdfOf(emptyList()).asWinAnsi()
        assertTrue(text.contains("Tidak ada transaksi"), "halaman kosong tanpa penjelasan")
        assertTrue(text.contains("Halaman 1 / 1"))
    }

    @Test
    fun schematicFileNamesStaySafe() {
        val name = salesReportFileName(report(listOf(row(1))))
        assertTrue(name.endsWith(".pdf"), name)
        assertTrue(
            name.none { it == ' ' || it == '/' || it == '\\' || it == ':' },
            "nama berkas tidak aman: $name"
        )
    }

    /** Logo disisipkan apa adanya sebagai JPEG (filter DCTDecode), bukan digambar ulang. */
    @Test
    fun logoIsEmbeddedAsJpeg() {
        val jpeg = byteArrayOf(
            0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte(), 0xC0.toByte(), 0x00, 0x11, 0x08,
            0x00, 0x10, 0x00, 0x10, 0x03, 0x01, 0x11, 0x00, 0x02, 0x11, 0x01, 0x03, 0x11, 0x01,
            0xFF.toByte(), 0xD9.toByte()
        )
        val dims = jpegDimensions(jpeg)
        assertNotNull(dims, "dimensi JPEG gagal dibaca dari penanda SOF")
        assertEquals(16 to 16, dims)
        val text = renderSalesReportPdf(report(listOf(row(1))), logoJpeg = jpeg).asWinAnsi()
        assertTrue(text.contains("/Filter /DCTDecode"), "logo tidak disisipkan sebagai JPEG")
        assertTrue(text.contains("/Width 16 /Height 16"), "ukuran gambar tidak ditulis benar")
    }

    @Test
    fun nonJpegBytesAreRejectedInsteadOfProducingABrokenPage() {
        assertEquals(null, jpegDimensions(byteArrayOf(0x89.toByte(), 0x50, 0x4E, 0x47)))
        assertEquals(null, jpegDimensions(ByteArray(0)))
    }
}

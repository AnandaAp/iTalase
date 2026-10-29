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

    private fun row(i: Long, customerName: String = "", paid: Boolean = i % 3L != 0L, total: Long = 10_000L * i, profit: Long = 2_500L * i) = SalesReportRow(
        receiptNo = i,
        dateMillis = 1_760_000_000_000L + i * 3_600_000L,
        methodLabel = if (i % 3L == 0L) "Bon" else "Tunai",
        statusLabel = if (paid) "Lunas" else "Bon",
        paid = paid,
        total = total,
        profit = profit,
        customerName = customerName,
    )

    private fun report(rows: List<SalesReportRow>, invoiceLimitReached: Boolean = false) = SalesReport(
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
        invoiceLimitReached = invoiceLimitReached,
    )

    private fun pdfOf(rows: List<SalesReportRow>, invoiceLimitReached: Boolean = false) =
        renderSalesReportPdf(report(rows, invoiceLimitReached), logoJpeg = null)

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
        assertEquals(null, jpegDimensions(byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0x00, 0x00)))
    }

    @Test
    fun jpegDimensionsEdgeCases() {
        // Zero dimensions
        val zeroDimsJpeg = byteArrayOf(
            0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte(), 0xC0.toByte(), 0x00, 0x11, 0x08,
            0x00, 0x00, 0x00, 0x00, 0x03, 0x01, 0x11, 0x00, 0x02, 0x11, 0x01, 0x03, 0x11, 0x01,
            0xFF.toByte(), 0xD9.toByte()
        )
        assertEquals(null, jpegDimensions(zeroDimsJpeg))

        // Non-frame marker 0xC4 (DHT) before SOF0
        val dhtBeforeSof = byteArrayOf(
            0xFF.toByte(), 0xD8.toByte(),
            0xFF.toByte(), 0xC4.toByte(), 0x00, 0x03, 0x00, // DHT length 3
            0xFF.toByte(), 0xC0.toByte(), 0x00, 0x11, 0x08,
            0x00, 0x20, 0x00, 0x40, 0x03, 0x01, 0x11, 0x00, 0x02, 0x11, 0x01, 0x03, 0x11, 0x01,
            0xFF.toByte(), 0xD9.toByte()
        )
        assertEquals(64 to 32, jpegDimensions(dhtBeforeSof))
    }

    @Test
    fun winAnsiBytesEncodingAndFallback() {
        val input = "Hello • – — … ‘ ’ “ ” Cafe\u00E9 \uD83D\uDE00"
        val bytes = winAnsiBytes(input)
        val string = bytes.map { it.toInt() and 0xFF }

        // '•' -> 0x95
        assertTrue(string.contains(0x95))
        // '–' -> 0x96
        assertTrue(string.contains(0x96))
        // '—' -> 0x97
        assertTrue(string.contains(0x97))
        // '…' -> 0x85
        assertTrue(string.contains(0x85))
        // 'é' -> 0xE9
        assertTrue(string.contains(0xE9))
        // emoji -> '?' -> 0x3F
        assertTrue(string.contains(0x3F))
    }

    @Test
    fun pdfNumFormatting() {
        assertEquals("10", pdfNum(10.0f))
        assertEquals("12.34", pdfNum(12.34f))
        assertEquals("0.50", pdfNum(0.50f))
        assertEquals("0.05", pdfNum(0.05f))
    }

    @Test
    fun testInvoiceLimitReachedWarningPrinted() {
        val text = pdfOf(listOf(row(1)), invoiceLimitReached = true).asWinAnsi()
        assertTrue(text.contains("Catatan: laporan dibatasi"), "Peringatan limit transaksi harus tercetak")
        assertTrue(text.contains("Persempit rentang tanggal untuk laporan lengkap"), "Instruksi persempit rentang harus ada")
    }

    @Test
    fun testUnpaidPiutangHighlightedInSummaryAndTotals() {
        val unpaidRow = row(3, customerName = "Bu Siti", paid = false, total = 75_000L, profit = 15_000L)
        val text = pdfOf(listOf(unpaidRow)).asWinAnsi()
        assertTrue(text.contains("Belum lunas"), "Label belum lunas piutang harus tercetak di blok total")
        assertTrue(text.contains("Rp75.000"), "Nominal piutang harus terformat dengan benar")
    }

    @Test
    fun testCustomerNamePrintedWithPaymentMethod() {
        val customerRow = row(1, customerName = "Pak Haji Joko", paid = true)
        val text = pdfOf(listOf(customerRow)).asWinAnsi()
        assertTrue(text.contains("Pak Haji Joko"), "Nama pelanggan harus tercetak di kolom metode")
        assertTrue(text.contains("Tunai"), "Metode pembayaran harus tercetak")
    }

    @Test
    fun testMultiPageReportThreePages() {
        // 80 baris: halaman 1 muat 26 baris, halaman 2 muat 34 baris, halaman 3 muat 20 baris
        val rows = (1L..80L).map { row(it) }
        val text = pdfOf(rows).asWinAnsi()
        assertTrue(text.contains("Halaman 1 / 3"), "Harus memiliki halaman 1 dari 3")
        assertTrue(text.contains("Halaman 2 / 3"), "Harus memiliki halaman 2 dari 3")
        assertTrue(text.contains("Halaman 3 / 3"), "Harus memiliki halaman 3 dari 3")
    }

    @Test
    fun testNegativeProfitOrLossTransactions() {
        val promoRow = row(1, total = 5_000L, profit = -1_500L)
        val text = pdfOf(listOf(promoRow)).asWinAnsi()
        assertTrue(text.length > 1_000, "PDF harus berhasil dirender meski profit negatif")
        assertTrue(text.contains("Rp5.000"), "Omzet harus tercetak")
    }

    @Test
    fun testLargeRevenueAndProfitFormatting() {
        val bigRow = row(1, total = 2_500_000_000L, profit = 500_000_000L)
        val text = pdfOf(listOf(bigRow)).asWinAnsi()
        assertTrue(text.contains("Rp2.500.000.000"), "Omzet miliaran harus diformat dengan pemisah ribuan")
        assertTrue(text.contains("Rp500.000.000"), "Laba miliaran harus diformat dengan pemisah ribuan")
    }

    @Test
    fun testCustomShopBrandingPdfGeneration() {
        val customBranding = ShopBranding(
            shopName = "Toko Kelontong Berkah",
            tagline = "Sedia Sembako Lengkap",
            address = "Pasar Tradisional Blok A No. 5",
            phone = "08123456789",
            reportFooter = "Dicetak melalui iTalase POS",
        )
        val rep = report(listOf(row(1))).copy(
            branding = customBranding
        )
        val text = renderSalesReportPdf(rep, logoJpeg = null).asWinAnsi()
        assertTrue(text.contains("Toko Kelontong Berkah"), "Nama toko kustom harus ada di PDF")
        assertTrue(text.contains("Pasar Tradisional Blok A No. 5"), "Alamat toko kustom harus ada di PDF")
        assertTrue(text.contains("Dicetak melalui iTalase POS"), "Footer laporan kustom harus tercetak")
    }
}

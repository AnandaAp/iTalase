package com.anlian.italase.shared.report

import com.anlian.italase.shared.util.formatDateTimeLong
import com.anlian.italase.shared.util.formatRupiah

// ════════════════════════════════════════════════════════════════════════
//  LAPORAN RIWAYAT TRANSAKSI (PDF)
// ════════════════════════════════════════════════════════════════════════
//  Yang dicetak: kop berlogo + nama app/warung, periode dan waktu unduh, ringkasan
//  angka, tabel transaksi bernomor halaman, lalu blok total. Datanya sudah
//  dihitung pemanggil — lapisan ini murni tata letak, jadi bisa diuji tanpa
//  database, Compose, atau jaringan.
//
//  Kenapa semua kolom rata kiri: font standar PDF tidak menyediakan metrik
//  lebar huruf, jadi rata kanan berarti menebak lebar teks dan angka bisa
//  saling tumpang tindih di laporan yang dirender pembaca lain. Kolom rata kiri
//  selalu sejajar karena setiap baris mulai di x yang sama — dan angka rupiah
//  tetap mudah dibandingkan karena berada di kolom yang sama.

/** Satu baris transaksi di laporan. Semua label sudah diterjemahkan pemanggil. */
data class SalesReportRow(
    /** Nomor struk seperti yang dilihat penjual (id baris di database). */
    val receiptNo: Long,
    /** Epoch millis transaksi; diformat di lapisan ini supaya bahasanya konsisten. */
    val dateMillis: Long,
    /** "Tunai" / "Transfer" / "Bon". */
    val methodLabel: String,
    /** "Lunas" / "Bon". */
    val statusLabel: String,
    /** true kalau [statusLabel] berarti sudah dibayar — mengubah warnanya. */
    val paid: Boolean,
    val total: Long,
    val profit: Long,
    /** Nama pelanggan; kosong untuk penjualan anonim. */
    val customerName: String = "",
)

/** Angka ringkasan di kop laporan. */
data class SalesReportTotals(
    val count: Int,
    val revenue: Long,
    val profit: Long,
    /** Sisa yang masih menjadi piutang; 0 kalau semua transaksi lunas. */
    val unpaid: Long,
)

/** Semua yang dibutuhkan laporan untuk menggambar dirinya sendiri. */
data class SalesReport(
    /** Nama aplikasi di kop, mis. "iTalase". */
    val appName: String,
    /** Kalimat kecil di bawah nama aplikasi. */
    val tagline: String,
    /** Judul laporan, mis. "Laporan Riwayat Transaksi". */
    val title: String,
    /** Rentang tanggal yang sudah diformat bahasa Indonesia. */
    val periodLabel: String,
    /** Saringan yang sedang aktif, mis. "Semua kategori". */
    val filterLabel: String,
    /** Epoch millis saat laporan dibuat. */
    val generatedAtMillis: Long,
    val rows: List<SalesReportRow>,
    val totals: SalesReportTotals,
    /** Catatan kaki, mis. sumber datanya. */
    val footerNote: String,
    /**
     * true kalau jumlah baris menyentuh [EXPORT_LIMIT], artinya laporan mungkin
     * memotong data lebih lama. Dicetak di blok total supaya user tidak mempercayai
     * laporan yang sebenarnya tidak lengkap.
     */
    val invoiceLimitReached: Boolean = false,
    /** Token branding dan identitas toko/warung (AUD-104). */
    val branding: ShopBranding = ShopBranding(
        shopName = appName,
        tagline = tagline,
        reportFooter = footerNote,
    ),
)

/** Lebar isi halaman setelah margin. */
private const val MARGIN = 40f
private const val RIGHT = A4_WIDTH - MARGIN
private const val CONTENT_WIDTH = RIGHT - MARGIN

/** Tinggi satu baris tabel dan tinggi blok total di bawah tabel. */
private const val ROW_HEIGHT = 17f
private const val TOTALS_BLOCK = 58f

/** Batas y (dari atas) agar baris terakhir tidak menabrak kaki halaman. */
private const val BOTTOM_LIMIT = 772f

/** Awal baris tabel di halaman pertama dan di halaman lanjutan. */
private const val ROWS_TOP_FIRST = 266f
private const val ROWS_TOP_NEXT = 132f

/** Kapasitas baris per halaman, sudah menyisakan ruang untuk blok total. */
private const val ROWS_FIRST_PAGE = 26
private const val ROWS_NEXT_PAGE = 34

/** Kolom tabel: x tetap per kolom, rata kiri. */
private const val COL_NO = MARGIN
private const val COL_DATE = MARGIN + 26f
private const val COL_METHOD = MARGIN + 204f
private const val COL_STATUS = MARGIN + 288f
private const val COL_TOTAL = MARGIN + 368f
private const val COL_PROFIT = MARGIN + 458f

private val BAND_ALT = PdfColor(0xF7, 0xF3, 0xE9)

/**
 * Gambar laporan jadi byte PDF.
 *
 * [logoJpeg] boleh null: kalau logonya tidak bisa dibaca, laporan tetap dibuat
 * dengan kop teks saja — laporan kosong karena gagal memuat gambar justru lebih
 * merugikan penjual.
 */
fun renderSalesReportPdf(report: SalesReport, logoJpeg: ByteArray?): ByteArray {
    val logoSize: Pair<Int, Int>? = logoJpeg?.let { jpegDimensions(it) }
    val totalPages: Int = pageCountFor(report.rows.size)
    val document = PdfDocument()
    var index = 0
    var pageNumber = 1
    while (index < report.rows.size || pageNumber == 1) {
        val first = pageNumber == 1
        val canvas = document.newPage()
        drawBand(canvas, report, logoJpeg, logoSize, first)
        var y = if (first) ROWS_TOP_FIRST else ROWS_TOP_NEXT
        if (first) {
            drawSummary(canvas, report)
            y = drawSectionHeading(canvas, y)
        }
        y = drawColumnHeader(canvas, y)
        val take = if (first) ROWS_FIRST_PAGE else ROWS_NEXT_PAGE
        var drawn = 0
        while (index < report.rows.size && drawn < take && y + ROW_HEIGHT <= BOTTOM_LIMIT) {
            drawRow(canvas, report.rows[index], y, drawn % 2 == 1)
            index++
            drawn++
            y += ROW_HEIGHT
        }
        if (report.rows.isEmpty() && first) {
            canvas.text(
                MARGIN,
                y + 12f,
                "Tidak ada transaksi pada rentang dan saringan ini.",
                10f,
                PdfColor.MUTED
            )
        } else if (index >= report.rows.size) {
            drawTotals(canvas, report, y)
        }
        drawFooter(canvas, report, pageNumber, totalPages)
        pageNumber++
    }
    return document.build()
}

/** Berapa halaman yang dibutuhkan; dihitung lebih dulu karena nomor halaman
 *  "x dari y" harus tahu totalnya sebelum halaman pertama digambar. */
private fun pageCountFor(rows: Int): Int {
    if (rows <= ROWS_FIRST_PAGE) return 1
    return 1 + (rows - ROWS_FIRST_PAGE + ROWS_NEXT_PAGE - 1) / ROWS_NEXT_PAGE
}

/**
 * Kop halaman: pita hijau merek, logo, nama aplikasi/toko, dan identitas laporan.
 *
 * Halaman lanjutan memakai pita yang lebih pendek — tidak perlu mengulang
 * tagline dan tanggal unduh di setiap halaman.
 */
private fun drawBand(
    canvas: PdfCanvas,
    report: SalesReport,
    logoJpeg: ByteArray?,
    logoSize: Pair<Int, Int>?,
    firstPage: Boolean,
) {
    val bandHeight = if (firstPage) 96f else 60f
    canvas.rect(0f, 0f, A4_WIDTH, bandHeight, PdfColor.BRAND)
    // Aksen specular Warung Liquid Glass di perbatasan bawah pita
    canvas.rule(0f, bandHeight, A4_WIDTH, PdfColor.BRAND_SOFT, 1.2f)

    val logoBox = 44f
    val logoTop = if (firstPage) 26f else 12f
    if (logoJpeg != null && logoSize != null) {
        canvas.image(logoJpeg, MARGIN, logoTop, logoBox, logoBox, logoSize.first, logoSize.second)
    }
    val textX = if (logoJpeg != null && logoSize != null) MARGIN + logoBox + 14f else MARGIN
    val shopTitle = report.branding.shopName.ifBlank { report.appName }
    val shopTagline = report.branding.tagline.ifBlank { report.tagline }

    canvas.text(textX, logoTop + 17f, shopTitle, 20f, PdfColor.WHITE, bold = true)
    if (firstPage) {
        val extraInfo = if (report.branding.address.isNotBlank()) " • ${report.branding.address}" else ""
        canvas.text(textX, logoTop + 34f, "$shopTagline$extraInfo", 8.5f, PdfColor.WHITE)
        canvas.text(COL_STATUS, 34f, report.title, 12f, PdfColor.WHITE, bold = true)
        canvas.text(COL_STATUS, 52f, "Periode: ${report.periodLabel}", 9f, PdfColor.WHITE)
        canvas.text(COL_STATUS, 66f, report.filterLabel, 9f, PdfColor.WHITE)
        canvas.text(
            COL_STATUS, 80f,
            "Diunduh ${formatDateTimeLong(report.generatedAtMillis)}", 8f, PdfColor.WHITE,
        )
    } else {
        canvas.text(COL_STATUS, logoTop + 14f, report.title, 10f, PdfColor.WHITE, bold = true)
        canvas.text(
            COL_STATUS,
            logoTop + 28f,
            "Periode: ${report.periodLabel}",
            8.5f,
            PdfColor.WHITE
        )
    }
}

/** Empat angka ringkasan dalam satu pita lembut. */
private fun drawSummary(canvas: PdfCanvas, report: SalesReport) {
    canvas.rect(MARGIN, 116f, CONTENT_WIDTH, 70f, PdfColor.BRAND_SOFT)
    val columns = listOf(
        "JUMLAH TRANSAKSI" to report.totals.count.toString(),
        "TOTAL OMZET" to formatRupiah(report.totals.revenue),
        "TOTAL LABA" to formatRupiah(report.totals.profit),
        "BELUM LUNAS" to formatRupiah(report.totals.unpaid),
    )
    val step = CONTENT_WIDTH / columns.size
    columns.forEachIndexed { i, (label, value) ->
        val x = MARGIN + 16f + i * step
        canvas.text(x, 138f, label, 7.5f, PdfColor.MUTED, bold = true)
        val valueColor = if (i == 3 && report.totals.unpaid > 0) PdfColor.ERROR else PdfColor.BRAND
        canvas.text(x, 160f, value, 13f, valueColor, bold = true)
    }
}

/** Judul bagian + garis pemisah; mengembalikan y untuk baris kop kolom. */
private fun drawSectionHeading(canvas: PdfCanvas, y: Float): Float {
    canvas.text(MARGIN, y, "RINCIAN TRANSAKSI", 10f, PdfColor.BRAND, bold = true)
    canvas.rule(MARGIN, y + 6f, RIGHT)
    return y + 28f
}

/** Nama kolom; diulang di setiap halaman supaya tabel tetap terbaca. */
private fun drawColumnHeader(canvas: PdfCanvas, y: Float): Float {
    canvas.text(COL_NO, y, "#", 8.5f, PdfColor.MUTED, bold = true)
    canvas.text(COL_DATE, y, "TANGGAL TRANSAKSI", 8.5f, PdfColor.MUTED, bold = true)
    canvas.text(COL_METHOD, y, "METODE", 8.5f, PdfColor.MUTED, bold = true)
    canvas.text(COL_STATUS, y, "STATUS", 8.5f, PdfColor.MUTED, bold = true)
    canvas.text(COL_TOTAL, y, "OMZET", 8.5f, PdfColor.MUTED, bold = true)
    canvas.text(COL_PROFIT, y, "LABA", 8.5f, PdfColor.MUTED, bold = true)
    canvas.rule(MARGIN, y + 3f, RIGHT)
    return y + 18f
}

/** Satu baris transaksi, dengan latar bergantian agar mata mudah menyusuri kolom. */
private fun drawRow(canvas: PdfCanvas, row: SalesReportRow, y: Float, alternate: Boolean) {
    if (alternate) canvas.rect(MARGIN, y - 11f, CONTENT_WIDTH, ROW_HEIGHT, BAND_ALT)
    canvas.text(COL_NO, y, row.receiptNo.toString(), 9f)
    canvas.text(COL_DATE, y, formatDateTimeLong(row.dateMillis), 9f)
    val method = if (row.customerName.isBlank()) row.methodLabel
    else "${row.methodLabel} • ${row.customerName}"
    canvas.text(COL_METHOD, y, method, 9f)
    canvas.text(
        COL_STATUS, y, row.statusLabel, 9f,
        if (row.paid) PdfColor.BRAND else PdfColor.ERROR, bold = true,
    )
    canvas.text(COL_TOTAL, y, formatRupiah(row.total), 9f)
    canvas.text(COL_PROFIT, y, formatRupiah(row.profit), 9f)
}

/** Blok total di bawah tabel; omzet dan laba sejajar dengan kolomnya. */
private fun drawTotals(canvas: PdfCanvas, report: SalesReport, y: Float) {
    val baseline = y + 22f
    canvas.rule(MARGIN, y - 2f, RIGHT, PdfColor.BRAND, 1f)
    canvas.text(
        MARGIN,
        baseline,
        "TOTAL ${report.totals.count} TRANSAKSI",
        10f,
        PdfColor.BLACK,
        bold = true
    )
    canvas.text(
        COL_TOTAL,
        baseline,
        formatRupiah(report.totals.revenue),
        10f,
        PdfColor.BLACK,
        bold = true
    )
    canvas.text(
        COL_PROFIT,
        baseline,
        formatRupiah(report.totals.profit),
        10f,
        PdfColor.BLACK,
        bold = true
    )
    if (report.totals.unpaid > 0) {
        canvas.text(
            MARGIN, baseline + 15f,
            "Belum lunas (piutang): ${formatRupiah(report.totals.unpaid)}",
            9f, PdfColor.ERROR,
        )
    }
    if (report.invoiceLimitReached) {
        canvas.text(
            MARGIN, baseline + 30f,
            "Catatan: laporan dibatasi $EXPORT_LIMIT transaksi terbaru. " +
                    "Persempit rentang tanggal untuk laporan lengkap.",
            8f, PdfColor.MUTED,
        )
    }
}

/** Kaki halaman: asal data di kiri, nomor halaman di kanan. */
private fun drawFooter(canvas: PdfCanvas, report: SalesReport, page: Int, total: Int) {
    // Garis kaki di 782 pt (= y 1210 px pada render 110 dpi): itu ~1,4 cm dari
    // tepi bawah A4, bukan menempel di tepi. Sebelumnya garisnya ditaruh terlalu
    // dekat (790 pt) sehingga pada render terlihat sebagai bingkai halaman.
    canvas.rule(MARGIN, 782f, RIGHT)
    val footerText = report.branding.reportFooter.ifBlank { report.footerNote }
    canvas.text(MARGIN, 797f, footerText, 7.5f, PdfColor.MUTED)
    canvas.text(COL_PROFIT, 797f, "Halaman $page / $total", 7.5f, PdfColor.MUTED, bold = true)
}

/** Nama berkas yang disarankan; tanggalnya format berkas (tanpa spasi/garis miring). */
fun salesReportFileName(report: SalesReport): String {
    val stamp = report.periodLabel.map { if (it.isLetterOrDigit()) it else '-' }
        .joinToString("")
        .replace("--", "-")
        .trim('-')
    val prefix = (report.branding.shopName.ifBlank { "iTalase" }).filter { it.isLetterOrDigit() }
    return "$prefix-Laporan-$stamp.pdf"
}

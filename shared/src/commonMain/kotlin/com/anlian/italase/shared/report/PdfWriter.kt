package com.anlian.italase.shared.report

// ════════════════════════════════════════════════════════════════════════
//  PENULIS PDF MINIMAL — murni commonMain, tanpa dependensi
// ════════════════════════════════════════════════════════════════════════
//  Kenapa ditulis sendiri, bukan per-platform (Android PdfDocument +
//  iOS UIGraphicsPDFRenderer):
//
//   • Satu implementasi berarti satu hasil. Dua implementasi kanvas akan
//     berbeda diam-diam (pengukuran teks, pemenggalan kata, margin) dan user
//     melihat laporan yang beda antara HP Android dan iPhone-nya.
//   • Bisa dikompilasi dan diuji di host mana pun, termasuk Windows. Target
//     iOS tidak bisa dikompilasi di sini, jadi implementasi `actual` untuk PDF
//     hanya akan jadi kode yang tidak pernah diverifikasi.
//   • Hasilnya ByteArray biasa, jadi penyimpanan/pembagian file tetap urusan
//     tipis per platform (lihat `sharePdf`).
//
//  Yang dipakai: PDF 1.4, font standar Helvetica (tidak perlu ditanam), dan
//  gambar JPEG lewat filter DCTDecode (byte JPEG ditempel apa adanya, tidak
//  perlu decode/re-encode).
//
//  Teks dikodekan sebagai WinAnsi (bukan UTF-8) karena font standar PDF
//  memakai single-byte encoding: "•" dan "–" punya kode khusus di WinAnsi,
//  dan karakter di luar itu diganti "?" supaya stream-nya tetap sah.

/**
 * Angka dengan dua desimal, tanpa `String.format`.
 *
 * `String.format` tidak ada di `commonMain` (JVM-only), dan memakainya berarti
 * kode ini gagal dikompilasi untuk iOS tanpa terlihat di host Windows. Karena
 * itu pembulatannya ditulis manual — sama seperti [formatRupiah].
 */
internal fun pdfNum(value: Float): String {
    val scaled: Long = kotlin.math.round(value * 100f).toLong()
    val whole: Long = scaled / 100
    val frac: Int = (scaled % 100).toInt()
    return if (frac == 0) "$whole" else "$whole.${frac.toString().padStart(2, '0')}"
}

/** Warna RGB 0..255 — sengaja bukan `androidx.compose.ui.graphics.Color`
 *  supaya lapisan ini tidak bergantung pada Compose dan bisa diuji murni. */
data class PdfColor(val r: Int, val g: Int, val b: Int) {
    val fill: String get() = "${pdfNum(r / 255f)} ${pdfNum(g / 255f)} ${pdfNum(b / 255f)} rg"
    val stroke: String get() = "${pdfNum(r / 255f)} ${pdfNum(g / 255f)} ${pdfNum(b / 255f)} RG"

    companion object {
        val BLACK = PdfColor(0x1C, 0x1B, 0x17)
        val MUTED = PdfColor(0x6B, 0x62, 0x50)
        val WHITE = PdfColor(0xFF, 0xFF, 0xFF)
        val BRAND = PdfColor(0x1D, 0x6B, 0x3C)
        val BRAND_SOFT = PdfColor(0xE3, 0xF1, 0xE7)
        val SAND = PdfColor(0xFD, 0xEB, 0xC8)
        val RULE = PdfColor(0xD8, 0xCF, 0xB6)
        val ERROR = PdfColor(0xB3, 0x26, 0x1E)
    }
}

/**
 * Penampung byte tumbuh sendiri.
 *
 * Ditulis dari nol karena `ByteArrayOutputStream` tidak ada di `commonMain`, dan
 * `String` tidak bisa dipakai: stream PDF harus byte tepat, sedangkan pengkodean
 * teksnya WinAnsi, bukan UTF-8.
 */
private class ByteSink(initialCapacity: Int = 4096) {
    private val out: ArrayList<Byte> = ArrayList(initialCapacity)
    val size: Int get() = out.size

    fun ascii(text: String) {
        for (c in text) out.add(c.code.toByte())
    }

    fun raw(bytes: ByteArray) {
        for (b in bytes) out.add(b)
    }

    fun toByteArray(): ByteArray = out.toByteArray()
}

/**
 * Ubah teks Kotlin jadi byte WinAnsi.
 *
 * Penting: ini BUKAN UTF-8. Font standar PDF (Helvetica) memakai single-byte
 * encoding, jadi "•" harus jadi 0x95 dan "–" jadi 0x96. Karakter yang tidak ada
 * di WinAnsi diganti "?" — laporan tetap sah dan bisa dibuka, dan tidak ada
 * teks yang hilang tanpa jejak, karena pemanggil laporan hanya memakai karakter
 * yang memang ada di WinAnsi.
 */
internal fun winAnsiBytes(text: String): ByteArray {
    val out = ByteArray(text.length)
    for (i in text.indices) {
        val c = text[i]
        out[i] = when {
            c.code < 0x80 -> c.code.toByte()
            c == '•' -> 0x95.toByte()
            c == '–' -> 0x96.toByte()
            c == '—' -> 0x97.toByte()
            c == '…' -> 0x85.toByte()
            c == '‘' -> 0x91.toByte()
            c == '’' -> 0x92.toByte()
            c == '“' -> 0x93.toByte()
            c == '”' -> 0x94.toByte()
            c.code in 0xA0..0xFF -> c.code.toByte()
            else -> '?'.code.toByte()
        }
    }
    return out
}

/** Escape teks untuk literal string PDF: `\`, `(` dan `)` harus dilindungi. */
private fun escapePdf(text: String): String =
    text.replace("\\", "\\\\").replace("(", "\\(").replace(")", "\\)")

/**
 * Satu halaman: koordinat PDF dalam poin, **origin di sudut kiri-bawah**.
 *
 * Pemanggil laporan bekerja dengan koordinat dari atas (lebih mudah dibaca),
 * jadi semua helper di sini yang menerima `y` memperlakukannya sebagai jarak
 * dari tepi ATAS halaman dan mengubahnya sendiri.
 */
class PdfCanvas(val width: Float, val height: Float) {
    private val sink = ByteSink()
    private val images = mutableListOf<EmbeddedImage>()

    internal class EmbeddedImage(
        val name: String,
        val bytes: ByteArray,
        val width: Int,
        val height: Int
    )

    internal fun collectImages(): List<EmbeddedImage> = images

    /** y dari atas → y PDF (dari bawah). */
    private fun up(y: Float): Float = height - y

    fun text(
        x: Float,
        y: Float,
        text: String,
        size: Float,
        color: PdfColor = PdfColor.BLACK,
        bold: Boolean = false,
    ) {
        if (text.isEmpty()) return
        sink.ascii("q ${color.fill} BT /${if (bold) "F2" else "F1"} $size Tf ")
        sink.ascii("1 0 0 1 ${x.pdf()} ${(up(y)).pdf()} Tm ")
        sink.ascii("(")
        sink.raw(winAnsiBytes(escapePdf(text)))
        sink.ascii(") Tj ET Q\n")
    }

    /**
     * Garis horizontal tipis — pemisah bagian dan baris tabel.
     *
     * [y] adalah jarak dari tepi atas, dan GARISNYA berada tepat di y itu. Sejak
     * perbaikan: garis tidak lagi menabrak garis-dasar teks. Sebelumnya pemanggil
     * mengoper jarak yang sama untuk teks dan garis, sehingga garis jatuh di
     * tengah glyph dan memotong angka di laporan hasil render.
     */
    fun rule(
        x1: Float,
        y: Float,
        x2: Float,
        color: PdfColor = PdfColor.RULE,
        widthPt: Float = 0.7f
    ) {
        sink.ascii("q ${color.stroke} $widthPt w ${x1.pdf()} ${up(y).pdf()} m ${x2.pdf()} ${up(y).pdf()} l S Q\n")
    }

    /** Kotak penuh warna — latar judul, blok ringkasan, badge status. */
    fun rect(x: Float, y: Float, w: Float, h: Float, color: PdfColor) {
        sink.ascii("q ${color.fill} ${x.pdf()} ${(up(y + h)).pdf()} ${w.pdf()} ${h.pdf()} re f Q\n")
    }

    /**
     * Sisipkan JPEG ke halaman.
     *
     * Tidak ada jalur transparansi di sini: PDF hanya mendukung alpha lewat
     * soft mask yang harus dibangun terpisah. Logo iTalase sudah berlatar penuh
     * (opaque), jadi tidak ada yang perlu ditembuskan.
     *
     * [width] dan [height] adalah ukuran piksel asli gambar — wajib, karena
     * objek gambar PDF harus menyebutkannya. Ambil dari [jpegDimensions].
     */
    fun image(jpeg: ByteArray, x: Float, y: Float, w: Float, h: Float, width: Int, height: Int) {
        val name = "Im${images.size + 1}"
        images.add(EmbeddedImage(name, jpeg, width, height))
        sink.ascii("q ${w.pdf()} 0 0 ${h.pdf()} ${x.pdf()} ${(up(y + h)).pdf()} cm /$name Do Q\n")
    }

    internal fun content(): ByteArray = sink.toByteArray()

    private fun Float.pdf(): String = pdfNum(this)
}

/**
 * Dokumen PDF 1.4 dengan font standar Helvetica/Helvetica-Bold.
 *
 * Dipakai laporan: buat halaman, gambar, lalu [build] untuk mendapat byte PDF.
 */
class PdfDocument {
    private val pages = mutableListOf<PdfCanvas>()

    fun newPage(width: Float = A4_WIDTH, height: Float = A4_HEIGHT): PdfCanvas =
        PdfCanvas(width, height).also { pages.add(it) }

    /** Byte PDF lengkap, siap ditulis ke file. */
    fun build(): ByteArray {
        // Nomor objek: 1 = Catalog, 2 = Pages, 3 = Font Helvetica,
        // 4 = Font Helvetica-Bold, lalu per halaman (page + content),
        // lalu semua gambar. Urutannya harus persis sama dengan yang ditulis.
        val objects = mutableListOf<ByteSink>()
        fun reserve(): Int = objects.size.let { objects.add(ByteSink()); it + 1 }

        val catalogId = reserve()
        val pagesId = reserve()
        val fontRegularId = reserve()
        val fontBoldId = reserve()
        val pageIds = pages.map { reserve() to reserve() }   // (page, content)
        val imageIds = mutableMapOf<String, Int>()
        pages.flatMap { it.collectImages() }.distinctBy { it.name }.forEach {
            imageIds[it.name] = reserve()
        }

        objects[catalogId - 1].ascii("<< /Type /Catalog /Pages $pagesId 0 R >>")
        objects[pagesId - 1].ascii(
            "<< /Type /Pages /Kids [${pageIds.joinToString(" ") { "${it.first} 0 R" }}] " +
                    "/Count ${pages.size} >>",
        )
        objects[fontRegularId - 1].ascii(
            "<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica /Encoding /WinAnsiEncoding >>",
        )
        objects[fontBoldId - 1].ascii(
            "<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica-Bold /Encoding /WinAnsiEncoding >>",
        )

        val xobjectRefs = imageIds.entries.joinToString(" ") { "/${it.key} ${it.value} 0 R" }
        pages.forEachIndexed { index, page ->
            val (pageId, contentId) = pageIds[index]
            val content = page.content()
            objects[pageId - 1].ascii(
                "<< /Type /Page /Parent $pagesId 0 R /MediaBox [0 0 ${page.width} ${page.height}] " +
                        "/Resources << /Font << /F1 $fontRegularId 0 R /F2 $fontBoldId 0 R >> " +
                        "/XObject << $xobjectRefs >> >> /Contents $contentId 0 R >>",
            )
            objects[contentId - 1].ascii("<< /Length ${content.size} >>\nstream\n")
            objects[contentId - 1].raw(content)
            objects[contentId - 1].ascii("\nendstream")
        }
        pages.flatMap { it.collectImages() }.distinctBy { it.name }.forEach { img ->
            val id = imageIds.getValue(img.name)
            objects[id - 1].ascii(
                "<< /Type /XObject /Subtype /Image /Width ${img.width} /Height ${img.height} " +
                        "/ColorSpace /DeviceRGB /BitsPerComponent 8 /Filter /DCTDecode " +
                        "/Length ${img.bytes.size} >>\nstream\n",
            )
            objects[id - 1].raw(img.bytes)
            objects[id - 1].ascii("\nendstream")
        }

        val file = ByteSink(1 shl 16)
        file.ascii("%PDF-1.4\n")
        // Komentar biner: memberi tahu alat bahwa berkas ini bukan teks murni.
        file.raw(
            byteArrayOf(
                0x25,
                0xE2.toByte(),
                0xE3.toByte(),
                0xCF.toByte(),
                0xD3.toByte(),
                0x0A
            )
        )
        val offsets = IntArray(objects.size + 1)
        objects.forEachIndexed { index, body ->
            offsets[index + 1] = file.size
            file.ascii("${index + 1} 0 obj\n")
            file.raw(body.toByteArray())
            file.ascii("\nendobj\n")
        }
        val xrefAt = file.size
        file.ascii("xref\n0 ${objects.size + 1}\n")
        file.ascii("0000000000 65535 f \n")
        for (i in 1..objects.size) {
            file.ascii(offsets[i].toString().padStart(10, '0') + " 00000 n \n")
        }
        file.ascii("trailer\n<< /Size ${objects.size + 1} /Root $catalogId 0 R >>\n")
        file.ascii("startxref\n$xrefAt\n%%EOF\n")
        return file.toByteArray()
    }
}

/** A4 dalam poin (210 × 297 mm). */
const val A4_WIDTH: Float = 595.28f
const val A4_HEIGHT: Float = 841.89f

/**
 * Ukuran sebuah JPEG dari penanda SOF-nya.
 *
 * Diperlukan karena objek gambar PDF wajib menyebut /Width dan /Height, dan
 * menuliskan angkanya sebagai konstanta berarti gambar akan terdistorsi begitu
 * berkas logonya diganti. `null` kalau berkasnya bukan JPEG yang bisa dibaca —
 * pemanggil memutuskan fallback-nya.
 */
fun jpegDimensions(bytes: ByteArray): Pair<Int, Int>? {
    if (bytes.size < 4 || bytes[0] != 0xFF.toByte() || bytes[1] != 0xD8.toByte()) return null
    var i = 2
    while (i + 9 < bytes.size) {
        if (bytes[i] != 0xFF.toByte()) {
            i++; continue
        }
        val marker = bytes[i + 1].toInt() and 0xFF
        // SOF0..SOF15, kecuali marker yang bukan frame (0xC4, 0xC8, 0xCC).
        if (marker in 0xC0..0xCF && marker != 0xC4 && marker != 0xC8 && marker != 0xCC) {
            val height = ((bytes[i + 5].toInt() and 0xFF) shl 8) or (bytes[i + 6].toInt() and 0xFF)
            val width = ((bytes[i + 7].toInt() and 0xFF) shl 8) or (bytes[i + 8].toInt() and 0xFF)
            return if (width > 0 && height > 0) width to height else null
        }
        if (marker == 0xD8 || marker == 0x01 || marker in 0xD0..0xD7) {
            i += 2; continue
        }
        val length = ((bytes[i + 2].toInt() and 0xFF) shl 8) or (bytes[i + 3].toInt() and 0xFF)
        if (length < 2) return null
        i += 2 + length
    }
    return null
}

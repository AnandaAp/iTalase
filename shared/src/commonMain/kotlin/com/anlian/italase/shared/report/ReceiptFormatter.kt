package com.anlian.italase.shared.report

import com.anlian.italase.shared.data.local.entity.PayMethod
import com.anlian.italase.shared.data.local.entity.TxnStatus
import com.anlian.italase.shared.data.repository.ShopRepository.SaleDetail
import com.anlian.italase.shared.util.formatDateTime
import com.anlian.italase.shared.util.formatDateTimeWithDay
import com.anlian.italase.shared.util.formatRupiah

/**
 * Pemformat nota transaksi warung untuk printer thermal (58mm/80mm) dan perpesanan WhatsApp (AUD-104).
 * Murni pure Kotlin di commonMain, tanpa dependensi platform khusus, sehingga dapat diuji secara mandiri.
 */
object ReceiptFormatter {

    /** Lebar standar kertas thermal 58mm (32 kolom monospace). */
    const val WIDTH_58MM = 32

    /** Lebar standar kertas thermal 80mm (42 kolom monospace). */
    const val WIDTH_80MM = 42

    /**
     * Format teks struk monospace untuk printer thermal Bluetooth / USB ESC-POS.
     * Mendukung pemotongan baris otomatis dan perataan dua kolom (kiri-kanan).
     */
    fun formatThermal(
        detail: SaleDetail,
        branding: ShopBranding = ShopBranding(),
        lineWidth: Int = WIDTH_58MM,
    ): String {
        val sale = detail.sale
        val items = detail.items
        val sb = StringBuilder()

        val div = "-".repeat(lineWidth)
        val doubleDiv = "=".repeat(lineWidth)

        // 1. Header Toko (Tengah)
        sb.appendLine(centerText(branding.shopName, lineWidth))
        if (branding.tagline.isNotBlank()) {
            sb.appendLine(centerText(branding.tagline, lineWidth))
        }
        if (branding.address.isNotBlank()) {
            sb.appendLine(centerText(branding.address, lineWidth))
        }
        if (branding.phone.isNotBlank()) {
            sb.appendLine(centerText("Telp: ${branding.phone}", lineWidth))
        }
        sb.appendLine(doubleDiv)

        // 2. Info Transaksi
        sb.appendLine(twoColumn("No: #${sale.id}", formatDateTime(sale.dateMillis), lineWidth))
        if (sale.customerName.isNotBlank()) {
            sb.appendLine("Pelanggan: ${sale.customerName}")
        }
        val methodLabel = when (sale.payMethod) {
            PayMethod.TUNAI -> "Tunai"
            PayMethod.TRANSFER -> "Transfer"
            PayMethod.BON -> "Bon (Hutang)"
            PayMethod.QRIS -> "QRIS"
            else -> sale.payMethod
        }
        val statusLabel = if (sale.status == TxnStatus.LUNAS) "LUNAS" else "BELUM LUNAS"
        sb.appendLine(twoColumn("Metode: $methodLabel", statusLabel, lineWidth))
        sb.appendLine(div)

        // 3. Daftar Barang
        items.forEach { item ->
            // Baris 1: Nama Produk
            sb.appendLine(item.productName)
            // Baris 2: Qty x Harga Jual (kiri) | Subtotal (kanan)
            val priceStr = "${item.qty} x ${formatRupiah(item.sellPrice)}"
            val subtotalStr = formatRupiah(item.subtotal)
            sb.appendLine(twoColumn("  $priceStr", subtotalStr, lineWidth))
        }
        sb.appendLine(div)

        // 4. Ringkasan Pembayaran
        sb.appendLine(twoColumn("TOTAL", formatRupiah(sale.total), lineWidth))

        val tendered = sale.tendered ?: sale.paidAmount
        if (tendered > 0) {
            sb.appendLine(twoColumn("Bayar", formatRupiah(tendered), lineWidth))
            val change = tendered - sale.total
            if (change > 0 || (change == 0L && sale.payMethod == PayMethod.TUNAI)) {
                sb.appendLine(twoColumn("Kembali", formatRupiah(change), lineWidth))
            }
        }

        if (sale.status != TxnStatus.LUNAS) {
            val remaining = (sale.total - sale.paidAmount).coerceAtLeast(0)
            sb.appendLine(twoColumn("Sisa Bon", formatRupiah(remaining), lineWidth))
        }

        if (sale.note.isNotBlank()) {
            sb.appendLine("Catatan: ${sale.note}")
        }
        sb.appendLine(div)

        // 5. Catatan Kaki / Ucapan Terima Kasih
        if (branding.receiptFooter.isNotBlank()) {
            sb.appendLine(centerText(branding.receiptFooter, lineWidth))
        }
        sb.appendLine(centerText("iTalase POS • Offline", lineWidth))

        return sb.toString()
    }

    /**
     * Format nota untuk pengiriman pesan WhatsApp / SMS dengan tanda markdown (*tebal*).
     */
    fun formatWhatsApp(
        detail: SaleDetail,
        branding: ShopBranding = ShopBranding(),
    ): String {
        val sale = detail.sale
        val items = detail.items
        val sb = StringBuilder()

        sb.appendLine("🧾 *STRUK PENJUALAN*")
        sb.appendLine("*${branding.shopName.uppercase()}*")
        if (branding.address.isNotBlank()) sb.appendLine(branding.address)
        if (branding.phone.isNotBlank()) sb.appendLine("📞 ${branding.phone}")
        sb.appendLine("--------------------------------")
        sb.appendLine("No. Struk : *#${sale.id}*")
        sb.appendLine("Waktu     : ${formatDateTimeWithDay(sale.dateMillis)}")
        if (sale.customerName.isNotBlank()) {
            sb.appendLine("Pelanggan : *${sale.customerName}*")
        }
        val methodLabel = when (sale.payMethod) {
            PayMethod.TUNAI -> "Tunai"
            PayMethod.TRANSFER -> "Transfer"
            PayMethod.BON -> "Bon"
            PayMethod.QRIS -> "QRIS"
            else -> sale.payMethod
        }
        val statusText = if (sale.status == TxnStatus.LUNAS) "LUNAS" else "BELUM LUNAS (BON)"
        sb.appendLine("Status    : *$statusText* ($methodLabel)")
        sb.appendLine("--------------------------------")

        items.forEach { item ->
            sb.appendLine("• *${item.productName}*")
            sb.appendLine("  ${item.qty} x ${formatRupiah(item.sellPrice)} = *${formatRupiah(item.subtotal)}*")
        }
        sb.appendLine("--------------------------------")
        sb.appendLine("💰 *TOTAL: ${formatRupiah(sale.total)}*")

        val tendered = sale.tendered ?: sale.paidAmount
        if (tendered > 0) {
            sb.appendLine("Uang Diterima: ${formatRupiah(tendered)}")
            val change = tendered - sale.total
            if (change > 0 || (change == 0L && sale.payMethod == PayMethod.TUNAI)) {
                sb.appendLine("Kembalian    : ${formatRupiah(change)}")
            }
        }

        if (sale.status != TxnStatus.LUNAS) {
            val remaining = (sale.total - sale.paidAmount).coerceAtLeast(0)
            sb.appendLine("⚠️ *Sisa Bon   : ${formatRupiah(remaining)}*")
        }

        if (sale.note.isNotBlank()) {
            sb.appendLine("Catatan: ${sale.note}")
        }
        sb.appendLine("--------------------------------")
        sb.appendLine(branding.receiptFooter)
        sb.appendLine("_Dibuat otomatis oleh iTalase POS_")

        return sb.toString()
    }

    /** Menengahkan teks dalam kolom selebar [width]. */
    fun centerText(text: String, width: Int): String {
        if (text.length >= width) return text
        val leftPad = (width - text.length) / 2
        val rightPad = width - text.length - leftPad
        return " ".repeat(leftPad) + text + " ".repeat(rightPad)
    }

    /** Membuat satu baris dengan teks kiri dan teks kanan yang terpisah spasi. */
    fun twoColumn(left: String, right: String, width: Int): String {
        val totalLen = left.length + right.length
        if (totalLen >= width) {
            // Jika melebihi lebar kolom, pertahankan spasi minimal 1
            return "$left $right"
        }
        val spaces = " ".repeat(width - totalLen)
        return "$left$spaces$right"
    }
}

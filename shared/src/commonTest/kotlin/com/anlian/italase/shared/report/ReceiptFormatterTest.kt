package com.anlian.italase.shared.report

import com.anlian.italase.shared.data.local.entity.PayMethod
import com.anlian.italase.shared.data.local.entity.SaleEntity
import com.anlian.italase.shared.data.local.entity.SaleItemEntity
import com.anlian.italase.shared.data.local.entity.TxnStatus
import com.anlian.italase.shared.data.repository.ShopRepository.SaleDetail
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Unit test pemformat nota transaksi warung untuk printer thermal dan WhatsApp (AUD-104).
 */
class ReceiptFormatterTest {

    private fun sampleDetail(
        receiptNo: Long = 101L,
        customerName: String = "Pak Joko",
        payMethod: String = PayMethod.TUNAI,
        status: String = TxnStatus.LUNAS,
        total: Long = 35_000L,
        tendered: Long = 50_000L,
        note: String = "Pesanan dibungkus",
    ): SaleDetail {
        val sale = SaleEntity(
            id = receiptNo,
            dateMillis = 1_760_000_000_000L,
            total = total,
            profit = 10_000L,
            payMethod = payMethod,
            paidAmount = if (status == TxnStatus.LUNAS) total else 20_000L,
            tendered = tendered,
            customerName = customerName,
            status = status,
            note = note,
        )
        val items = listOf(
            SaleItemEntity(
                id = 1L,
                saleId = receiptNo,
                productId = 10L,
                productName = "Kopi Susu Gula Aren",
                category = "Minuman",
                qty = 2,
                buyPrice = 8_000L,
                sellPrice = 12_000L,
                subtotal = 24_000L,
                profit = 8_000L,
            ),
            SaleItemEntity(
                id = 2L,
                saleId = receiptNo,
                productId = 11L,
                productName = "Roti Bakar Cokelat",
                category = "Makanan",
                qty = 1,
                buyPrice = 7_000L,
                sellPrice = 11_000L,
                subtotal = 11_000L,
                profit = 4_000L,
            ),
        )
        return SaleDetail(sale, items)
    }

    @Test
    fun testThermal58mmFormattingCarriesAllSections() {
        val detail = sampleDetail()
        val text = ReceiptFormatter.formatThermal(detail, lineWidth = ReceiptFormatter.WIDTH_58MM)

        // Verifikasi nama toko bawaan
        assertTrue(text.contains(REPORT_APP_NAME))
        // Verifikasi nomor struk & tanggal
        assertTrue(text.contains("#101"))
        // Verifikasi nama pelanggan
        assertTrue(text.contains("Pak Joko"))
        // Verifikasi barang
        assertTrue(text.contains("Kopi Susu Gula Aren"))
        assertTrue(text.contains("Roti Bakar Cokelat"))
        // Verifikasi nominal
        assertTrue(text.contains("Rp35.000"))
        assertTrue(text.contains("Rp50.000"))
        assertTrue(text.contains("Rp15.000")) // Kembalian = 50.000 - 35.000
        // Verifikasi status lunas
        assertTrue(text.contains("LUNAS"))
        // Verifikasi catatan kasir
        assertTrue(text.contains("Pesanan dibungkus"))
    }

    @Test
    fun testThermal58mmLineWidthConstraints() {
        val detail = sampleDetail()
        val text = ReceiptFormatter.formatThermal(detail, lineWidth = ReceiptFormatter.WIDTH_58MM)
        val lines = text.lines().filter { it.isNotBlank() }

        // Setiap baris pemisah harus tepat 32 karakter
        val dividerLines = lines.filter { it.all { c -> c == '-' || c == '=' } }
        assertTrue(dividerLines.isNotEmpty())
        dividerLines.forEach { line ->
            assertEquals(32, line.length, "Panjang baris pembatas thermal 58mm harus 32 karakter")
        }
    }

    @Test
    fun testThermal80mmFormatting() {
        val detail = sampleDetail()
        val text = ReceiptFormatter.formatThermal(detail, lineWidth = ReceiptFormatter.WIDTH_80MM)

        assertTrue(text.contains("No: #101"))
        assertTrue(text.contains("Rp35.000"))
        val lines = text.lines().filter { it.isNotBlank() && it.all { c -> c == '-' } }
        assertTrue(lines.isNotEmpty())
        lines.forEach { line ->
            assertEquals(42, line.length, "Panjang pembatas thermal 80mm harus 42 karakter")
        }
    }

    @Test
    fun testUnpaidBonThermalReceiptFormatting() {
        val bonDetail = sampleDetail(
            status = TxnStatus.BON,
            payMethod = PayMethod.BON,
            total = 50_000L,
            tendered = 20_000L,
        )
        val text = ReceiptFormatter.formatThermal(bonDetail)

        assertTrue(text.contains("BELUM LUNAS"), "Status harus BELUM LUNAS pada transaksi bon")
        assertTrue(text.contains("Sisa Bon"), "Harus menampilkan sisa bon piutang")
        assertTrue(text.contains("Rp30.000"), "Sisa bon harus Rp30.000 (50.000 - 20.000)")
    }

    @Test
    fun testCustomShopBrandingApplication() {
        val customBranding = ShopBranding(
            shopName = "Warung Madura Berkah",
            tagline = "Buka 24 Jam Nonstop",
            address = "Jl. Kaliurang KM 5 No. 12",
            phone = "0812-9876-5432",
            receiptFooter = "Matur Nuwun Sanget!",
        )
        val detail = sampleDetail()
        val thermal = ReceiptFormatter.formatThermal(detail, branding = customBranding)

        assertTrue(thermal.contains("Warung Madura Berkah"))
        assertTrue(thermal.contains("Buka 24 Jam Nonstop"))
        assertTrue(thermal.contains("Jl. Kaliurang KM 5 No. 12"))
        assertTrue(thermal.contains("0812-9876-5432"))
        assertTrue(thermal.contains("Matur Nuwun Sanget!"))

        val wa = ReceiptFormatter.formatWhatsApp(detail, branding = customBranding)
        assertTrue(wa.contains("*WARUNG MADURA BERKAH*"))
        assertTrue(wa.contains("0812-9876-5432"))
        assertTrue(wa.contains("Matur Nuwun Sanget!"))
    }

    @Test
    fun testWhatsAppFormattingStructure() {
        val detail = sampleDetail()
        val wa = ReceiptFormatter.formatWhatsApp(detail)

        assertTrue(wa.contains("🧾 *STRUK PENJUALAN*"))
        assertTrue(wa.contains("No. Struk : *#101*"))
        assertTrue(wa.contains("Pelanggan : *Pak Joko*"))
        assertTrue(wa.contains("💰 *TOTAL: Rp35.000*"))
        assertTrue(wa.contains("Kembalian    : Rp15.000"))
        assertTrue(wa.contains("• *Kopi Susu Gula Aren*"))
        assertTrue(wa.contains("• *Roti Bakar Cokelat*"))
    }

    @Test
    fun testHelperCenterAndTwoColumn() {
        val centered = ReceiptFormatter.centerText("iTalase", 20)
        assertEquals(20, centered.length)
        assertTrue(centered.trim() == "iTalase")

        val col = ReceiptFormatter.twoColumn("TOTAL", "Rp10.000", 20)
        assertEquals(20, col.length)
        assertTrue(col.startsWith("TOTAL"))
        assertTrue(col.endsWith("Rp10.000"))
    }

    @Test
    fun testThermalReceiptQrisPayment() {
        val qrisDetail = sampleDetail(
            receiptNo = 105L,
            payMethod = PayMethod.QRIS,
            status = TxnStatus.LUNAS,
            total = 35_000L,
            tendered = 35_000L,
        )
        val text = ReceiptFormatter.formatThermal(qrisDetail, lineWidth = ReceiptFormatter.WIDTH_58MM)

        assertTrue(text.contains("Metode: QRIS"), "Harus menampilkan Metode: QRIS")
        assertTrue(text.contains("LUNAS"), "Status harus LUNAS")
        assertTrue(text.contains("TOTAL"), "Harus mencetak baris TOTAL")
        assertTrue(text.contains("Rp35.000"), "Harus memuat nominal total")
        assertTrue(text.contains("Bayar"), "Harus mencetak baris Bayar")
        // Kembalian Rp0 tidak dicetak pada pembayaran QRIS
        val lines = text.lines()
        val hasKembali = lines.any { it.contains("Kembali") }
        assertEquals(false, hasKembali, "Metode QRIS tidak boleh mencetak baris Kembali Rp0")
    }

    @Test
    fun testWhatsAppReceiptQrisPayment() {
        val qrisDetail = sampleDetail(
            receiptNo = 105L,
            payMethod = PayMethod.QRIS,
            status = TxnStatus.LUNAS,
            total = 35_000L,
            tendered = 35_000L,
        )
        val wa = ReceiptFormatter.formatWhatsApp(qrisDetail)

        assertTrue(wa.contains("Status    : *LUNAS* (QRIS)"), "Status WA harus *LUNAS* (QRIS)")
        assertTrue(wa.contains("💰 *TOTAL: Rp35.000*"))
        assertTrue(wa.contains("Uang Diterima: Rp35.000"))
        val hasKembalian = wa.lines().any { it.contains("Kembalian") }
        assertEquals(false, hasKembalian, "Metode QRIS tidak boleh mencetak baris Kembalian pada WA")
    }

    @Test
    fun testThermalReceiptCashExactChangeZeroPrintsKembali() {
        val cashExact = sampleDetail(
            receiptNo = 106L,
            payMethod = PayMethod.TUNAI,
            status = TxnStatus.LUNAS,
            total = 35_000L,
            tendered = 35_000L,
        )
        val text = ReceiptFormatter.formatThermal(cashExact, lineWidth = ReceiptFormatter.WIDTH_58MM)
        assertTrue(text.contains("Metode: Tunai"))
        assertTrue(text.contains("Kembali"), "Metode Tunai harus tetap mencetak Kembali Rp0 jika uang pas")
        assertTrue(text.contains("Rp0"))

        val wa = ReceiptFormatter.formatWhatsApp(cashExact)
        assertTrue(wa.contains("Kembalian    : Rp0"), "Metode Tunai WA harus tetap mencetak Kembalian Rp0")
    }

    @Test
    fun testReceiptFormattingCoversAllPayMethods() {
        val methods = listOf(PayMethod.TUNAI, PayMethod.TRANSFER, PayMethod.BON, PayMethod.QRIS, "DEBIT")
        methods.forEach { method ->
            val detail = sampleDetail(payMethod = method)
            val thermal = ReceiptFormatter.formatThermal(detail)
            val wa = ReceiptFormatter.formatWhatsApp(detail)
            assertTrue(thermal.isNotBlank(), "Format thermal untuk $method tidak boleh kosong")
            assertTrue(wa.isNotBlank(), "Format WA untuk $method tidak boleh kosong")
        }
    }
}

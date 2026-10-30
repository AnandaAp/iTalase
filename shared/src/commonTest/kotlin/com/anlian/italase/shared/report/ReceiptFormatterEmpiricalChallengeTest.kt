package com.anlian.italase.shared.report

import com.anlian.italase.shared.data.local.entity.PayMethod
import com.anlian.italase.shared.data.local.entity.SaleEntity
import com.anlian.italase.shared.data.local.entity.SaleItemEntity
import com.anlian.italase.shared.data.local.entity.TxnStatus
import com.anlian.italase.shared.data.repository.ShopRepository.SaleDetail
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Empirical Adversarial Challenger Test Suite for Milestone M5:
 * Stress-testing ReceiptFormatter under boundary conditions, non-ASCII/Unicode text,
 * extreme currency amounts, empty item lists, thermal column widths, and QRIS vs Cash semantics.
 */
class ReceiptFormatterEmpiricalChallengeTest {

    private fun buildDetail(
        receiptNo: Long = 9999L,
        customerName: String = "Test Customer",
        payMethod: String = PayMethod.QRIS,
        status: String = TxnStatus.LUNAS,
        total: Long = 50_000L,
        tendered: Long = 50_000L,
        paidAmount: Long = total,
        note: String = "",
        items: List<SaleItemEntity> = listOf(
            SaleItemEntity(
                id = 1L,
                saleId = receiptNo,
                productId = 1L,
                productName = "Adversarial Item",
                category = "Umum",
                qty = 1,
                buyPrice = 10_000L,
                sellPrice = 50_000L,
                subtotal = 50_000L,
                profit = 40_000L,
            )
        ),
    ): SaleDetail {
        val sale = SaleEntity(
            id = receiptNo,
            dateMillis = 1_760_000_000_000L,
            total = total,
            profit = 40_000L,
            payMethod = payMethod,
            paidAmount = paidAmount,
            tendered = tendered,
            customerName = customerName,
            status = status,
            note = note,
        )
        return SaleDetail(sale, items)
    }

    // =========================================================================
    // 1. Extreme Amounts & Boundary Numbers
    // =========================================================================

    @Test
    fun testZeroTotalAndTenderedTransaction() {
        val detail = buildDetail(
            total = 0L,
            tendered = 0L,
            paidAmount = 0L,
            payMethod = PayMethod.QRIS,
            items = emptyList(),
        )
        val thermal = ReceiptFormatter.formatThermal(detail)
        val wa = ReceiptFormatter.formatWhatsApp(detail)

        assertTrue(thermal.contains("TOTAL"), "Thermal must have TOTAL line")
        assertTrue(thermal.contains("Rp0"), "Thermal must format Rp0 correctly")
        assertFalse(thermal.contains("Bayar"), "Tendered 0 should not output Bayar line")
        assertFalse(thermal.contains("Kembali"), "Zero tendered must not output Kembali")

        assertTrue(wa.contains("TOTAL: Rp0"))
        assertFalse(wa.contains("Uang Diterima"))
        assertFalse(wa.contains("Kembalian"))
    }

    @Test
    fun testQuadrillionAmountFormattingDoesNotOverflow() {
        val largeAmount = 9_000_000_000_000_000L // 9 Quadrillion IDR
        val detail = buildDetail(
            total = largeAmount,
            tendered = largeAmount,
            paidAmount = largeAmount,
            payMethod = PayMethod.QRIS,
        )
        val thermal = ReceiptFormatter.formatThermal(detail)
        val wa = ReceiptFormatter.formatWhatsApp(detail)

        // 9.000.000.000.000.000
        assertTrue(thermal.contains("9.000.000.000.000.000"))
        assertTrue(wa.contains("9.000.000.000.000.000"))
        // QRIS exact must not output Kembali
        assertFalse(thermal.lines().any { it.contains("Kembali") })
    }

    @Test
    fun testNegativeTotalRefundScenario() {
        val refundAmount = -75_000L
        val detail = buildDetail(
            total = refundAmount,
            tendered = 0L,
            paidAmount = refundAmount,
            payMethod = PayMethod.TUNAI,
        )
        val thermal = ReceiptFormatter.formatThermal(detail)
        val wa = ReceiptFormatter.formatWhatsApp(detail)

        assertTrue(thermal.contains("-Rp75.000"), "Must handle negative currency representation")
        assertTrue(wa.contains("-Rp75.000"))
    }

    // =========================================================================
    // 2. Empty and High-Volume Item Lists
    // =========================================================================

    @Test
    fun testEmptyItemListDoesNotCrash() {
        val detail = buildDetail(items = emptyList())
        val thermal = ReceiptFormatter.formatThermal(detail)
        val wa = ReceiptFormatter.formatWhatsApp(detail)

        assertTrue(thermal.isNotBlank())
        assertTrue(wa.isNotBlank())
        assertTrue(thermal.contains("iTalase POS • Offline"))
    }

    @Test
    fun testHighVolume200ItemsStress() {
        val items = (1..200).map { i ->
            SaleItemEntity(
                id = i.toLong(),
                saleId = 100L,
                productId = i.toLong(),
                productName = "Item Ke-$i",
                category = "Stress",
                qty = i,
                buyPrice = 1_000L,
                sellPrice = 2_000L,
                subtotal = i * 2_000L,
                profit = i * 1_000L,
            )
        }
        val detail = buildDetail(items = items, total = items.sumOf { it.subtotal })
        val thermal = ReceiptFormatter.formatThermal(detail)
        val wa = ReceiptFormatter.formatWhatsApp(detail)

        assertTrue(thermal.contains("Item Ke-1"))
        assertTrue(thermal.contains("Item Ke-200"))
        assertTrue(wa.contains("Item Ke-200"))
    }

    // =========================================================================
    // 3. Non-ASCII, Unicode, Multilingual, and Injection Strings
    // =========================================================================

    @Test
    fun testMultilingualUnicodeBrandingAndCustomerNames() {
        val unicodeBranding = ShopBranding(
            shopName = "سوبرماركت البركة • 🏪 便利店",
            tagline = "Kafe & Kopi ☕ München Schön",
            address = "Jl. Sudirman № 42, Москва 123",
            phone = "+62 811-234-5678",
            receiptFooter = "🙏 Terimakasih! ありがとうございます! شكرا",
        )
        val detail = buildDetail(
            customerName = "Renée Müller / 李小龙 / أحمد",
            note = "<script>alert('XSS')</script> -- ' OR 1=1;",
        )
        val thermal = ReceiptFormatter.formatThermal(detail, branding = unicodeBranding)
        val wa = ReceiptFormatter.formatWhatsApp(detail, branding = unicodeBranding)

        assertTrue(thermal.contains("سوبرماركت البركة"))
        assertTrue(thermal.contains("便利店"))
        assertTrue(thermal.contains("Kafe & Kopi ☕ München Schön"))
        assertTrue(thermal.contains("Renée Müller / 李小龙 / أحمد"))
        assertTrue(thermal.contains("<script>alert('XSS')</script> -- ' OR 1=1;"))

        assertTrue(wa.contains("سوبرماركت البركة"))
        assertTrue(wa.contains("便利店"))
        assertTrue(wa.contains("Renée Müller / 李小龙 / أحمد"))
    }

    @Test
    fun testVeryLongStringsWrappingGracefully() {
        val longName = "A".repeat(120)
        val customBranding = ShopBranding(shopName = longName)
        val detail = buildDetail(customerName = "B".repeat(80))

        val thermal = ReceiptFormatter.formatThermal(detail, branding = customBranding)
        assertTrue(thermal.contains(longName), "Very long shop name should be present without crashing")
        assertTrue(thermal.contains("B".repeat(80)))
    }

    // =========================================================================
    // 4. Strict 32-Column Thermal Printer Bounds
    // =========================================================================

    @Test
    fun testTwoColumnWidthConstraintsExactness() {
        // When total length < width: padded to exact width
        val res1 = ReceiptFormatter.twoColumn("TOTAL", "Rp35.000", 32)
        assertEquals(32, res1.length)
        assertTrue(res1.startsWith("TOTAL"))
        assertTrue(res1.endsWith("Rp35.000"))

        // When total length == width - 1 (31): padded with 1 space to exact width (32)
        val left15 = "123456789012345"
        val right16 = "ABCDEFGHIKLMNOPQ"
        val res2 = ReceiptFormatter.twoColumn(left15, right16, 32)
        assertEquals(32, res2.length)
        assertEquals("$left15 $right16", res2)

        // When total length >= width (e.g. 16 + 16 = 32): to avoid fusing columns without spaces,
        // it preserves at least 1 space ("$left $right" -> 33 chars).
        val left16 = "1234567890123456"
        val res3Boundary = ReceiptFormatter.twoColumn(left16, right16, 32)
        assertEquals("$left16 $right16", res3Boundary)
        assertEquals(33, res3Boundary.length)

        // When total length > width: separates by single space
        val longLeft = "TOTAL_LONG_LABEL_12345678"
        val longRight = "Rp999.999.999.999"
        val res4 = ReceiptFormatter.twoColumn(longLeft, longRight, 32)
        assertEquals("$longLeft $longRight", res4)
        assertTrue(res4.length > 32)
    }

    @Test
    fun testCenterTextWidthConstraintsExactness() {
        // Shorter than width
        val centered = ReceiptFormatter.centerText("WARUNG", 32)
        assertEquals(32, centered.length)
        assertEquals("WARUNG", centered.trim())

        // Exactly equal to width
        val exact = "12345678901234567890123456789012"
        val resExact = ReceiptFormatter.centerText(exact, 32)
        assertEquals(32, resExact.length)
        assertEquals(exact, resExact)

        // Longer than width: returns as-is
        val longer = "12345678901234567890123456789012345"
        val resLonger = ReceiptFormatter.centerText(longer, 32)
        assertEquals(longer, resLonger)
    }

    @Test
    fun testQrisStandardTransactionThermalLinesFit32Columns() {
        val detail = buildDetail(
            receiptNo = 101L,
            payMethod = PayMethod.QRIS,
            status = TxnStatus.LUNAS,
            total = 35_000L,
            tendered = 35_000L,
        )
        val text = ReceiptFormatter.formatThermal(detail, lineWidth = ReceiptFormatter.WIDTH_58MM)
        val lines = text.lines()

        // 1. Divider lines must be exactly 32
        val dividers = lines.filter { it.startsWith("---") || it.startsWith("===") }
        assertTrue(dividers.isNotEmpty())
        dividers.forEach { assertEquals(32, it.length) }

        // 2. Info Transaksi line: "Metode: QRIS               LUNAS" must be exactly 32
        val methodLine = lines.firstOrNull { it.contains("Metode: QRIS") }
        assertTrue(methodLine != null)
        assertEquals(32, methodLine.length, "Baris Metode: QRIS LUNAS harus tepat 32 karakter")

        // 3. Ringkasan total & bayar: must be exactly 32
        val totalLine = lines.firstOrNull { it.startsWith("TOTAL") }
        assertTrue(totalLine != null)
        assertEquals(32, totalLine.length, "Baris TOTAL harus tepat 32 karakter")

        val bayarLine = lines.firstOrNull { it.startsWith("Bayar") }
        assertTrue(bayarLine != null)
        assertEquals(32, bayarLine.length, "Baris Bayar harus tepat 32 karakter")
    }

    // =========================================================================
    // 5. Cash vs QRIS Exact Payment & Change Semantics
    // =========================================================================

    @Test
    fun testCashExactPaymentOutputsKembaliRp0() {
        val cashExact = buildDetail(
            payMethod = PayMethod.TUNAI,
            status = TxnStatus.LUNAS,
            total = 50_000L,
            tendered = 50_000L,
        )
        val thermal = ReceiptFormatter.formatThermal(cashExact)
        val kembaliLine = thermal.lines().firstOrNull { it.contains("Kembali") }
        assertTrue(kembaliLine != null, "Cash exact payment MUST contain Kembali line")
        assertEquals(32, kembaliLine.length, "Kembali line must be 32 chars")
        assertTrue(kembaliLine.contains("Rp0"))

        val wa = ReceiptFormatter.formatWhatsApp(cashExact)
        assertTrue(wa.contains("Kembalian    : Rp0"), "Cash exact payment in WA MUST contain Kembalian: Rp0")
    }

    @Test
    fun testQrisExactPaymentSuppressesKembali() {
        val qrisExact = buildDetail(
            payMethod = PayMethod.QRIS,
            status = TxnStatus.LUNAS,
            total = 50_000L,
            tendered = 50_000L,
        )
        val thermal = ReceiptFormatter.formatThermal(qrisExact)
        assertFalse(thermal.lines().any { it.contains("Kembali") }, "QRIS exact payment MUST NOT print Kembali")

        val wa = ReceiptFormatter.formatWhatsApp(qrisExact)
        assertFalse(wa.lines().any { it.contains("Kembalian") }, "QRIS exact payment in WA MUST NOT print Kembalian")
    }

    @Test
    fun testQrisOverpaymentPrintsKembaliWithDifference() {
        val qrisOverpaid = buildDetail(
            payMethod = PayMethod.QRIS,
            status = TxnStatus.LUNAS,
            total = 50_000L,
            tendered = 60_000L, // Customer scanned & paid 60.000
        )
        val thermal = ReceiptFormatter.formatThermal(qrisOverpaid)
        val kembaliLine = thermal.lines().firstOrNull { it.contains("Kembali") }
        assertTrue(kembaliLine != null, "QRIS overpayment MUST print Kembali")
        assertTrue(kembaliLine.contains("Rp10.000"))

        val wa = ReceiptFormatter.formatWhatsApp(qrisOverpaid)
        assertTrue(wa.contains("Kembalian    : Rp10.000"))
    }

    @Test
    fun testQrisPartialPaymentUnderBonShowsSisaBonNotNegativeKembali() {
        val qrisBon = buildDetail(
            payMethod = PayMethod.QRIS,
            status = TxnStatus.BON,
            total = 100_000L,
            tendered = 40_000L,
            paidAmount = 40_000L,
        )
        val thermal = ReceiptFormatter.formatThermal(qrisBon)
        // Must NOT print negative change
        assertFalse(thermal.lines().any { it.contains("Kembali") })
        // Must print Sisa Bon
        assertTrue(thermal.contains("Sisa Bon"))
        assertTrue(thermal.contains("Rp60.000"))

        val wa = ReceiptFormatter.formatWhatsApp(qrisBon)
        assertFalse(wa.lines().any { it.contains("Kembalian") })
        assertTrue(wa.contains("Sisa Bon   : Rp60.000"))
    }

    @Test
    fun testTransferAndDebitPaymentMethodsSuppressZeroKembali() {
        val transferExact = buildDetail(
            payMethod = PayMethod.TRANSFER,
            status = TxnStatus.LUNAS,
            total = 75_000L,
            tendered = 75_000L,
        )
        val thermalTransfer = ReceiptFormatter.formatThermal(transferExact)
        assertFalse(thermalTransfer.lines().any { it.contains("Kembali") }, "Transfer exact payment must suppress Kembali")

        val debitExact = buildDetail(
            payMethod = "DEBIT",
            status = TxnStatus.LUNAS,
            total = 75_000L,
            tendered = 75_000L,
        )
        val thermalDebit = ReceiptFormatter.formatThermal(debitExact)
        assertFalse(thermalDebit.lines().any { it.contains("Kembali") }, "Debit exact payment must suppress Kembali")
    }
}

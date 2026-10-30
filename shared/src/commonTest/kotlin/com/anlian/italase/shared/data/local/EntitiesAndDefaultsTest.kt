package com.anlian.italase.shared.data.local

import com.anlian.italase.shared.data.ProductCategories
import com.anlian.italase.shared.data.ShopDefaults
import com.anlian.italase.shared.data.StockRules
import com.anlian.italase.shared.data.local.entity.DebtEntity
import com.anlian.italase.shared.data.local.entity.DebtPaymentEntity
import com.anlian.italase.shared.data.local.entity.DebtStatus
import com.anlian.italase.shared.data.local.entity.ExpenseEntity
import com.anlian.italase.shared.data.local.entity.PayMethod
import com.anlian.italase.shared.data.local.entity.ProductEntity
import com.anlian.italase.shared.data.local.entity.QrisConfigEntity
import com.anlian.italase.shared.data.local.entity.SaleEntity
import com.anlian.italase.shared.data.local.entity.SaleItemEntity
import com.anlian.italase.shared.data.local.entity.TxnStatus
import com.anlian.italase.shared.ui.navigation.Routes
import com.anlian.italase.shared.util.currentTimeMillis
import com.anlian.italase.shared.util.dayRangeMillis
import com.anlian.italase.shared.util.daysBetweenInclusive
import com.anlian.italase.shared.util.endOfDayMillis
import com.anlian.italase.shared.util.endOfLocalDayMillis
import com.anlian.italase.shared.util.formatDateLong
import com.anlian.italase.shared.util.formatDateRange
import com.anlian.italase.shared.util.formatDateShort
import com.anlian.italase.shared.util.formatDateTime
import com.anlian.italase.shared.util.formatDateTimeLong
import com.anlian.italase.shared.util.formatDateTimeWithDay
import com.anlian.italase.shared.util.formatTime
import com.anlian.italase.shared.util.localDateOf
import com.anlian.italase.shared.util.localDateTimeOf
import com.anlian.italase.shared.util.minusDays
import com.anlian.italase.shared.util.nowMillis
import com.anlian.italase.shared.util.shiftDays
import com.anlian.italase.shared.util.startOfDayMillis
import com.anlian.italase.shared.util.startOfLocalDayMillis
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class EntitiesAndDefaultsTest {

    @Test
    fun testDefaultsAndRules() {
        assertEquals("pcs", ShopDefaults.UNIT)
        assertEquals("Operasional", ShopDefaults.CATEGORY)
        assertEquals("Pelanggan", ShopDefaults.CUSTOMER)

        assertEquals(3, StockRules.LOW_STOCK_FALLBACK)

        assertEquals("Makanan", ProductCategories.FOOD)
        assertEquals("Minuman", ProductCategories.DRINK)
        assertEquals("Sembako", ProductCategories.STAPLE)
        assertEquals("Rumah Tangga", ProductCategories.HOUSEHOLD)
        assertEquals("Kesehatan", ProductCategories.HEALTH)
        assertEquals("Lainnya", ProductCategories.OTHER)
        assertEquals(6, ProductCategories.ALL.size)

        assertEquals("italase.db", DbFiles.NAME)

        assertEquals("products", Tables.PRODUCTS)
        assertEquals("sales", Tables.SALES)
        assertEquals("sale_items", Tables.SALE_ITEMS)
        assertEquals("debts", Tables.DEBTS)
        assertEquals("debt_payments", Tables.DEBT_PAYMENTS)
        assertEquals("expenses", Tables.EXPENSES)
        assertEquals("qris_config", Tables.QRIS_CONFIG)
    }

    @Test
    fun testNavigationRoutes() {
        assertEquals("home", Routes.HOME)
        assertEquals("kasir", Routes.CASHIER)
        assertEquals("stok", Routes.STOCK)
        assertEquals("riwayat", Routes.HISTORY)
        assertEquals("riwayat_detail", Routes.HISTORY_DETAIL)
        assertEquals("bon", Routes.DEBT)
        assertEquals("biaya", Routes.EXPENSE)
        assertEquals("more", Routes.MORE)

        assertTrue(Routes.isTabRoute(Routes.HOME))
        assertTrue(Routes.isTabRoute(Routes.CASHIER))
        assertTrue(Routes.isTabRoute(Routes.STOCK))
        assertTrue(Routes.isTabRoute(Routes.DEBT))
        assertTrue(Routes.isTabRoute(Routes.MORE))
        assertFalse(Routes.isTabRoute(Routes.HISTORY))
        assertFalse(Routes.isTabRoute(Routes.EXPENSE))
        assertFalse(Routes.isTabRoute(null))

        assertTrue(Routes.hasBackButton(Routes.HISTORY))
        assertTrue(Routes.hasBackButton(Routes.EXPENSE))
        assertFalse(Routes.hasBackButton(Routes.HOME))
        assertFalse(Routes.hasBackButton(Routes.CASHIER))
        assertFalse(Routes.hasBackButton(Routes.HISTORY_DETAIL))
        assertFalse(Routes.hasBackButton(null))
    }

    @Test
    fun testEntitiesAndStatuses() {
        assertEquals("TUNAI", PayMethod.TUNAI)
        assertEquals("TRANSFER", PayMethod.TRANSFER)
        assertEquals("BON", PayMethod.BON)
        assertEquals("QRIS", PayMethod.QRIS)

        assertEquals("LUNAS", TxnStatus.LUNAS)
        assertEquals("BON", TxnStatus.BON)

        assertEquals("BELUM_LUNAS", DebtStatus.BELUM_LUNAS)
        assertEquals("LUNAS", DebtStatus.LUNAS)

        val prod = ProductEntity(
            id = 10, name = "Kopi", barcode = "123", category = "Minuman",
            buyPrice = 2000, sellPrice = 4000, stock = 15, minStock = 5,
            unit = "cup", createdAt = 100, updatedAt = 200
        )
        assertEquals(10, prod.id)
        assertEquals("Kopi", prod.name)
        assertEquals(15, prod.stock)
        assertEquals(prod, prod.copy())

        val sale = SaleEntity(
            id = 1, dateMillis = 100, total = 4000, profit = 2000,
            payMethod = PayMethod.TUNAI, paidAmount = 4000, tendered = 5000,
            customerName = "Budi", status = TxnStatus.LUNAS, note = "Oke"
        )
        assertEquals(1, sale.id)
        assertEquals(4000, sale.total)
        assertEquals(sale, sale.copy())

        val item = SaleItemEntity(
            id = 1, saleId = 1, productId = 10, productName = "Kopi",
            category = "Minuman", qty = 1, buyPrice = 2000, sellPrice = 4000,
            subtotal = 4000, profit = 2000
        )
        assertEquals(1, item.id)
        assertEquals(4000, item.subtotal)
        assertEquals(item, item.copy())

        val debt = DebtEntity(
            id = 1, customerName = "Ali", phone = "08123", saleId = 1,
            totalAmount = 10000, paidAmount = 4000, remaining = 6000,
            status = DebtStatus.BELUM_LUNAS, dateMillis = 100, note = ""
        )
        assertEquals(1, debt.id)
        assertEquals(6000, debt.remaining)
        assertEquals(debt, debt.copy())

        val payment = DebtPaymentEntity(id = 1, debtId = 1, amount = 2000, dateMillis = 100, note = "Cicil")
        assertEquals(1, payment.id)
        assertEquals(2000, payment.amount)
        assertEquals(payment, payment.copy())

        val exp = ExpenseEntity(id = 1, name = "Listrik", category = "Operasional", amount = 50000, dateMillis = 100, note = "Token")
        assertEquals(1, exp.id)
        assertEquals(50000, exp.amount)
        assertEquals(exp, exp.copy())

        val b1 = byteArrayOf(1, 2, 3)
        val b2 = byteArrayOf(1, 2, 3)
        val b3 = byteArrayOf(4, 5, 6)
        val qris1 = QrisConfigEntity(
            id = 1, isEnabled = true, imageBytes = b1, mimeType = "image/jpeg",
            promptDismissCount = 1, updatedAt = 100L
        )
        val qris2 = QrisConfigEntity(
            id = 1, isEnabled = true, imageBytes = b2, mimeType = "image/jpeg",
            promptDismissCount = 1, updatedAt = 100L
        )
        val qris3 = QrisConfigEntity(
            id = 1, isEnabled = true, imageBytes = b3, mimeType = "image/jpeg",
            promptDismissCount = 1, updatedAt = 100L
        )
        val qrisNull = QrisConfigEntity(id = 1, isEnabled = false, imageBytes = null)

        assertEquals(1, qris1.id)
        assertTrue(qris1.isEnabled)
        assertEquals("image/jpeg", qris1.mimeType)
        assertEquals(1, qris1.promptDismissCount)
        assertEquals(100L, qris1.updatedAt)

        assertEquals(qris1, qris2)
        assertEquals(qris1.hashCode(), qris2.hashCode())
        assertTrue(qris1 == qris1)
        assertFalse(qris1.equals(null))
        assertFalse(qris1.equals("string"))

        assertFalse(qris1 == qris3)
        assertFalse(qris1 == qrisNull)
        assertFalse(qrisNull == qris1)
        assertFalse(qris1 == qris1.copy(id = 2))
        assertFalse(qris1 == qris1.copy(isEnabled = false))
        assertFalse(qris1 == qris1.copy(mimeType = "image/png"))
        assertFalse(qris1 == qris1.copy(promptDismissCount = 2))
        assertFalse(qris1 == qris1.copy(updatedAt = 200L))

        assertEquals(qrisNull, qrisNull.copy())
        assertEquals(qrisNull.hashCode(), qrisNull.copy().hashCode())

        assertTrue(qris1.toString().contains("3 bytes"))
        assertTrue(qrisNull.toString().contains("null"))
    }

    @Test
    fun testTimeUtilCoverage() {
        val now = nowMillis()
        assertTrue(now > 0)
        assertTrue(currentTimeMillis() > 0)

        val ldt = localDateTimeOf(now)
        val ld = localDateOf(now)
        assertEquals(ldt.date, ld)

        val start = startOfLocalDayMillis(now)
        val end = endOfLocalDayMillis(now)
        assertTrue(start < end)
        assertTrue(now in start..end)

        assertNull(startOfDayMillis(null))
        assertNotNull(startOfDayMillis(ld))

        assertNull(endOfDayMillis(null))
        assertNotNull(endOfDayMillis(ld))

        val (r1, r2) = dayRangeMillis(null, null)
        assertEquals(Long.MIN_VALUE, r1)
        assertEquals(Long.MAX_VALUE, r2)

        val (r3, r4) = dayRangeMillis(ld, ld)
        assertTrue(r3 <= r4)

        val date1 = LocalDate(2026, 1, 15)
        val date2 = LocalDate(2026, 1, 20)
        val date3 = LocalDate(2026, 2, 10)
        val date4 = LocalDate(2027, 3, 5)

        assertEquals("Semua tanggal", formatDateRange(null, null))
        assertTrue(formatDateRange(date1, null).contains("15 Januari 2026"))
        assertTrue(formatDateRange(null, date2).contains("20 Januari 2026"))
        assertEquals("15 Januari 2026", formatDateRange(date1, date1))
        assertTrue(formatDateRange(date1, date2).contains("15 - 20 Januari 2026"))
        assertTrue(formatDateRange(date1, date3).contains("15 Januari 2026"))
        assertTrue(formatDateRange(date1, date4).contains("15 Januari 2026"))

        val timeStr = formatTime(now)
        assertTrue(timeStr.contains(":"))

        val dateLong = formatDateLong(now)
        assertTrue(dateLong.isNotEmpty())

        val dateTimeLong = formatDateTimeLong(now)
        assertTrue(dateTimeLong.contains("•"))

        val dateShort = formatDateShort(now)
        assertTrue(dateShort.isNotEmpty())

        val dateTime = formatDateTime(now)
        assertTrue(dateTime.contains("•"))

        val withDay = formatDateTimeWithDay(now)
        assertTrue(withDay.contains(","))

        assertEquals(6, daysBetweenInclusive(date1, date2))
        assertNull(daysBetweenInclusive(date1, null))

        val shifted = date1.shiftDays(5)
        assertEquals(20, shifted.day)

        val minused = date2.minusDays(5)
        assertEquals(15, minused.day)
    }
}

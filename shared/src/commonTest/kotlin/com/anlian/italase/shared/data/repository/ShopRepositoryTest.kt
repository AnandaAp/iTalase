package com.anlian.italase.shared.data.repository

import com.anlian.italase.shared.data.ShopDefaults
import com.anlian.italase.shared.data.local.entity.DebtStatus
import com.anlian.italase.shared.data.local.entity.PayMethod
import com.anlian.italase.shared.data.local.entity.ProductEntity
import com.anlian.italase.shared.data.local.entity.TxnStatus
import com.anlian.italase.shared.util.nowMillis
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ShopRepositoryTest {

    private fun setupRepo(): Pair<FakeDatabase, ShopRepository> {
        val db = FakeDatabase()
        val repo = ShopRepository(db)
        return db to repo
    }

    @Test
    fun testProductCrudAndStockAdjustment() = runBlocking {
        val (db, repo) = setupRepo()

        // 1. Save new product
        val newProd = ProductEntity(name = "Kopi Susu", buyPrice = 5000, sellPrice = 10000, stock = 20)
        val id1 = repo.saveProduct(newProd)
        assertEquals(1L, id1)

        val fetched = db.pDao.getById(id1)
        assertNotNull(fetched)
        assertEquals("Kopi Susu", fetched.name)
        assertEquals(20, fetched.stock)

        // 2. Update existing product
        val updatedProd = fetched.copy(name = "Kopi Susu Gula Aren", sellPrice = 12000)
        repo.saveProduct(updatedProd)
        val fetchedUpdated = db.pDao.getById(id1)
        assertEquals("Kopi Susu Gula Aren", fetchedUpdated?.name)
        assertEquals(12000, fetchedUpdated?.sellPrice)

        // 3. Adjust stock positive
        repo.adjustStock(id1, 5)
        assertEquals(25, db.pDao.getById(id1)?.stock)

        // 4. Adjust stock negative
        repo.adjustStock(id1, -10)
        assertEquals(15, db.pDao.getById(id1)?.stock)

        // 5. Adjust stock below 0 coerces to 0
        repo.adjustStock(id1, -50)
        assertEquals(0, db.pDao.getById(id1)?.stock)

        // 6. Adjust non-existent product does not fail
        repo.adjustStock(9999L, 5)

        // 7. Observe low stock and products
        val lowStock = repo.observeLowStock().first()
        assertTrue(lowStock.any { it.id == id1 })

        // 8. Delete product
        repo.deleteProduct(fetchedUpdated!!)
        assertNull(db.pDao.getById(id1))
    }

    @Test
    fun testCheckoutValidations() = runBlocking {
        val (db, repo) = setupRepo()

        val prod = ProductEntity(id = 1L, name = "Teh", buyPrice = 2000, sellPrice = 5000, stock = 5)
        db.pDao.upsert(prod)

        // 1. Empty cart
        val exEmpty = assertFailsWith<ShopException> {
            repo.checkout(emptyList(), paidAmount = 5000, payMethod = PayMethod.TUNAI, customerName = "A")
        }
        assertEquals(ErrorCode.EMPTY_CART, exEmpty.code)

        // 2. Product missing
        val exGone = assertFailsWith<ShopException> {
            repo.checkout(
                listOf(CartLine(ProductEntity(id = 999L, name = "Hilang", buyPrice = 1, sellPrice = 2, stock = 10), 1)),
                paidAmount = 2000,
                payMethod = PayMethod.TUNAI,
                customerName = "A"
            )
        }
        assertEquals(ErrorCode.PRODUCT_GONE, exGone.code)

        // 3. Invalid Qty
        val exQty = assertFailsWith<ShopException> {
            repo.checkout(
                listOf(CartLine(prod, 0)),
                paidAmount = 5000,
                payMethod = PayMethod.TUNAI,
                customerName = "A"
            )
        }
        assertEquals(ErrorCode.QTY_INVALID, exQty.code)

        // 4. Insufficient stock
        val exStock = assertFailsWith<ShopException> {
            repo.checkout(
                listOf(CartLine(prod, 10)),
                paidAmount = 50000,
                payMethod = PayMethod.TUNAI,
                customerName = "A"
            )
        }
        assertEquals(ErrorCode.STOCK_SHORT, exStock.code)
    }

    @Test
    fun testCheckoutLunasAndBon() = runBlocking {
        val (db, repo) = setupRepo()

        val p1 = ProductEntity(id = 1L, name = "Beras 5kg", buyPrice = 50000, sellPrice = 65000, stock = 10)
        db.pDao.upsert(p1)

        // Checkout LUNAS (paid >= total)
        val saleId = repo.checkout(
            lines = listOf(CartLine(p1, 2)),
            paidAmount = 150000,
            payMethod = PayMethod.TUNAI,
            customerName = "Budi",
            note = "Lunas"
        )
        assertEquals(1L, saleId)

        val sale = db.sDao.getSale(saleId)
        assertNotNull(sale)
        assertEquals(130000L, sale.total)
        assertEquals(30000L, sale.profit)
        assertEquals(TxnStatus.LUNAS, sale.status)
        assertEquals(130000L, sale.paidAmount)
        assertEquals(150000L, sale.tendered)
        assertEquals(8, db.pDao.getById(1L)?.stock)

        val items = repo.saleItems(saleId)
        assertEquals(1, items.size)
        assertEquals(2, items[0].qty)

        // Checkout BON (paid < total)
        val p2 = ProductEntity(id = 2L, name = "Minyak 2L", buyPrice = 25000, sellPrice = 30000, stock = 5)
        db.pDao.upsert(p2)

        val bonSaleId = repo.checkout(
            lines = listOf(CartLine(p2, 1)),
            paidAmount = 10000,
            payMethod = PayMethod.TUNAI, // Will become BON because underpaid
            customerName = "Siti",
            note = "Hutang 20rb"
        )

        val bonSale = db.sDao.getSale(bonSaleId)
        assertNotNull(bonSale)
        assertEquals(30000L, bonSale.total)
        assertEquals(TxnStatus.BON, bonSale.status)
        assertEquals(PayMethod.BON, bonSale.payMethod)
        assertEquals(10000L, bonSale.paidAmount)

        val debts = db.dDao.observeActive().first()
        assertEquals(1, debts.size)
        assertEquals("Siti", debts[0].customerName)
        assertEquals(20000L, debts[0].remaining)
        assertEquals(DebtStatus.BELUM_LUNAS, debts[0].status)
    }

    @Test
    fun testDebtsAndPayments() = runBlocking {
        val (db, repo) = setupRepo()

        // 1. Validations for manual debt
        assertFailsWith<ShopException> { repo.saveManualDebt("", 50000, "note") }
        assertFailsWith<ShopException> { repo.saveManualDebt("Joko", 0, "note") }
        assertFailsWith<ShopException> { repo.saveManualDebt("Joko", -10, "note") }

        // 2. Save valid manual debt
        val debtId = repo.saveManualDebt("Joko", 100000, "Pinjam")
        assertEquals(1L, debtId)

        assertEquals(100000L, repo.totalPiutang())
        val debtList = repo.observeDebts().first()
        assertEquals(1, debtList.size)
        val activeDebts = repo.observeActiveDebts().first()
        assertEquals(1, activeDebts.size)

        // 3. Payment validations
        assertFailsWith<ShopException> { repo.payDebt(debtId, 0) }
        assertFailsWith<ShopException> { repo.payDebt(debtId, -5000) }
        assertFailsWith<ShopException> { repo.payDebt(9999L, 50000) }

        // 4. Partial payment
        val effective1 = repo.payDebt(debtId, 40000, "Cicil 1")
        assertEquals(40000L, effective1)

        val debtAfter1 = db.dDao.getById(debtId)
        assertEquals(40000L, debtAfter1?.paidAmount)
        assertEquals(60000L, debtAfter1?.remaining)
        assertEquals(DebtStatus.BELUM_LUNAS, debtAfter1?.status)
        assertEquals(60000L, repo.totalPiutang())

        // 5. Overpayment settles full remaining
        val effective2 = repo.payDebt(debtId, 100000, "Lunas lebih")
        assertEquals(60000L, effective2)

        val debtAfter2 = db.dDao.getById(debtId)
        assertEquals(100000L, debtAfter2?.paidAmount)
        assertEquals(0L, debtAfter2?.remaining)
        assertEquals(DebtStatus.LUNAS, debtAfter2?.status)
        assertEquals(0L, repo.totalPiutang())

        // 6. Pay already paid debt throws ALREADY_PAID
        val exPaid = assertFailsWith<ShopException> { repo.payDebt(debtId, 10000) }
        assertEquals(ErrorCode.ALREADY_PAID, exPaid.code)
    }

    @Test
    fun testExpenses() = runBlocking {
        val (db, repo) = setupRepo()

        // Validations
        assertFailsWith<ShopException> { repo.saveExpense("", "Ops", 10000, "") }
        assertFailsWith<ShopException> { repo.saveExpense("Listrik", "Ops", 0, "") }
        assertFailsWith<ShopException> { repo.saveExpense("Listrik", "Ops", -10, "") }

        // Valid save with fallback default category
        val expId1 = repo.saveExpense("Listrik", "", 50000, "Token")
        val exp1 = db.eDao.expenses.value.find { it.id == expId1 }
        assertEquals(ShopDefaults.CATEGORY, exp1?.category)
        assertEquals(50000L, exp1?.amount)

        // Save with explicit category
        val expId2 = repo.saveExpense("Plastik", "Operasional", 15000, "")
        assertEquals(2, repo.observeExpenses().first().size)

        // Delete expense
        repo.deleteExpense(expId1)
        assertEquals(1, repo.observeExpenses().first().size)
    }

    @Test
    fun testHistoryReportsAndSummaries() = runBlocking {
        val (db, repo) = setupRepo()
        val now = nowMillis()

        val p1 = ProductEntity(id = 1L, name = "Kopi Hitam", category = "Minuman", buyPrice = 3000, sellPrice = 5000, stock = 50)
        val p2 = ProductEntity(id = 2L, name = "Roti Coklat", category = "Makanan", buyPrice = 4000, sellPrice = 7000, stock = 50)
        db.pDao.upsert(p1)
        db.pDao.upsert(p2)

        val saleId = repo.checkout(
            lines = listOf(CartLine(p1, 3), CartLine(p2, 2)),
            paidAmount = 50000,
            payMethod = PayMethod.TUNAI,
            customerName = "Anto",
            note = "Bungkus"
        )

        // Observe sales in range
        val allSales = repo.observeSalesInRange(from = now - 10000, to = now + 10000, category = null).first()
        assertEquals(1, allSales.size)

        val catSales = repo.observeSalesInRange(from = now - 10000, to = now + 10000, category = "Minuman").first()
        assertEquals(1, catSales.size)

        val recent = repo.observeRecentSales().first()
        assertEquals(1, recent.size)

        val byCat = repo.observeSalesByCategory("Minuman").first()
        assertEquals(1, byCat.size)

        val categories = repo.observeItemCategories().first()
        assertTrue(categories.isNotEmpty())

        // Sale Detail
        val detail = repo.saleDetail(saleId)
        assertNotNull(detail)
        assertEquals("Anto", detail.sale.customerName)
        assertEquals(2, detail.items.size)

        assertNull(repo.saleDetail(9999L))

        // Update sale info
        repo.updateSaleInfo(saleId, "Anto Wijaya", "Bungkus plastik")
        val updatedDetail = repo.saleDetail(saleId)
        assertEquals("Anto Wijaya", updatedDetail?.sale?.customerName)
        assertEquals("Bungkus plastik", updatedDetail?.sale?.note)

        // Update payment info to BON (underpaid: 10000 of 29000)
        repo.updateSaleInfo(saleId, "Anto Wijaya", "Sebagian bon", PayMethod.BON, 10000L)
        val bonDetail = repo.saleDetail(saleId)
        assertEquals(PayMethod.BON, bonDetail?.sale?.payMethod)
        assertEquals(TxnStatus.BON, bonDetail?.sale?.status)
        assertEquals(10000L, bonDetail?.sale?.paidAmount)
        assertEquals(10000L, bonDetail?.sale?.tendered)
        val createdDebt = db.dDao.getBySaleId(saleId)
        assertNotNull(createdDebt)
        assertEquals(19000L, createdDebt.remaining)
        assertEquals(DebtStatus.BELUM_LUNAS, createdDebt.status)

        // Update payment info back to TUNAI lunas (tendered 30000 >= 29000)
        repo.updateSaleInfo(saleId, "Anto Wijaya", "Sudah lunas tunai", PayMethod.TUNAI, 30000L)
        val lunasDetail = repo.saleDetail(saleId)
        assertEquals(PayMethod.TUNAI, lunasDetail?.sale?.payMethod)
        assertEquals(TxnStatus.LUNAS, lunasDetail?.sale?.status)
        assertEquals(29000L, lunasDetail?.sale?.paidAmount)
        assertEquals(30000L, lunasDetail?.sale?.tendered)
        assertNull(db.dDao.getBySaleId(saleId))

        // Update nonexistent sale does not throw
        repo.updateSaleInfo(9999L, "Nobody", "None")

        // Expense for Day Summary
        repo.saveExpense("Bensin", "Transport", 10000, "")

        // Day Summary
        val summary = repo.daySummary(now)
        assertEquals(29000L, summary.omzet) // 3*5000 + 2*7000 = 15000 + 14000 = 29000
        assertEquals(12000L, summary.profit) // 3*2000 + 2*3000 = 6000 + 6000 = 12000
        assertEquals(1, summary.trxCount)
        assertEquals(10000L, summary.expense)
        assertEquals(2000L, summary.bersih) // 12000 - 10000 = 2000

        // Top products
        val top = repo.topProducts(now)
        assertEquals(2, top.size)
        assertEquals("Kopi Hitam", top[0].productName)
        assertEquals(3, top[0].q)
    }

    @Test
    fun testQrisConfigCrudAndToggles() = runBlocking {
        val (db, repo) = setupRepo()

        // 1. Initial state check
        val initial = repo.getQrisConfig()
        assertNotNull(initial)
        assertEquals(false, initial.isEnabled)
        assertNull(initial.imageBytes)
        assertEquals(0, initial.promptDismissCount)

        // 2. Save new QRIS photo
        val sampleBytes = byteArrayOf(1, 2, 3, 4, 5)
        repo.saveQrisConfig(sampleBytes, "image/png")

        val saved = repo.getQrisConfig()
        assertNotNull(saved)
        assertTrue(saved.isEnabled)
        assertTrue(sampleBytes.contentEquals(saved.imageBytes))
        assertEquals("image/png", saved.mimeType)
        assertTrue(saved.updatedAt > 0L)

        // 3. Observe reactive Flow
        val observed = repo.observeQrisConfig().first()
        assertEquals(saved, observed)

        // 4. Toggle disabled
        repo.setQrisEnabled(false)
        val disabled = repo.getQrisConfig()
        assertNotNull(disabled)
        assertEquals(false, disabled.isEnabled)
        assertNotNull(disabled.imageBytes)

        // 5. Toggle enabled again
        repo.setQrisEnabled(true)
        val reEnabled = repo.getQrisConfig()
        assertNotNull(reEnabled)
        assertEquals(true, reEnabled.isEnabled)

        // 6. Delete config
        repo.deleteQrisConfig()
        val deleted = repo.getQrisConfig()
        assertNull(deleted)

        // 7. Toggle enabled on null record safely creates it
        repo.setQrisEnabled(true)
        val afterNullEnable = repo.getQrisConfig()
        assertNotNull(afterNullEnable)
        assertEquals(1, afterNullEnable.id)
        assertTrue(afterNullEnable.isEnabled)
        assertNull(afterNullEnable.imageBytes)

        // 8. Delete again and save on null record safely re-creates
        repo.deleteQrisConfig()
        val newBytes = byteArrayOf(9, 8, 7)
        repo.saveQrisConfig(newBytes)
        val recreated = repo.getQrisConfig()
        assertNotNull(recreated)
        assertEquals(1, recreated.id)
        assertTrue(recreated.isEnabled)
        assertTrue(newBytes.contentEquals(recreated.imageBytes))
        assertEquals("image/jpeg", recreated.mimeType)
    }

    @Test
    fun testQrisPromptDismissalCounter() = runBlocking {
        val (db, repo) = setupRepo()

        // Dismiss 1
        repo.dismissQrisPrompt()
        assertEquals(1, repo.getQrisConfig()?.promptDismissCount)

        // Dismiss 2
        repo.dismissQrisPrompt()
        assertEquals(2, repo.getQrisConfig()?.promptDismissCount)

        // Dismiss 3
        repo.dismissQrisPrompt()
        val config3 = repo.getQrisConfig()
        assertEquals(3, config3?.promptDismissCount)

        // Verify prompt eligibility condition: imageBytes == null && promptDismissCount < 3
        val isPromptEligible = config3?.imageBytes == null && (config3?.promptDismissCount ?: 0) < 3
        assertFalse(isPromptEligible)

        // Dismiss on deleted/null record
        repo.deleteQrisConfig()
        repo.dismissQrisPrompt()
        val afterNullDismiss = repo.getQrisConfig()
        assertNotNull(afterNullDismiss)
        assertEquals(1, afterNullDismiss.promptDismissCount)
    }

    @Test
    fun testCheckoutWithQrisPayMethod() = runBlocking {
        val (db, repo) = setupRepo()
        val prod = ProductEntity(id = 1L, name = "Kopi Susu", buyPrice = 5000, sellPrice = 10000, stock = 10)
        db.pDao.upsert(prod)

        // Checkout with PayMethod.QRIS (Lunas)
        val saleId = repo.checkout(
            lines = listOf(CartLine(prod, 2)),
            paidAmount = 20000,
            payMethod = PayMethod.QRIS,
            customerName = "Rudi Scan",
            note = "QRIS Dinamis",
        )
        assertEquals(1L, saleId)

        val sale = db.sDao.getSale(saleId)
        assertNotNull(sale)
        assertEquals(PayMethod.QRIS, sale.payMethod)
        assertEquals(TxnStatus.LUNAS, sale.status)
        assertEquals(20000L, sale.total)
        assertEquals(20000L, sale.paidAmount)
        assertEquals(20000L, sale.tendered)
        assertEquals("Rudi Scan", sale.customerName)
        assertEquals(8, db.pDao.getById(1L)?.stock)

        // Update payment info to QRIS
        repo.updateSaleInfo(saleId, "Rudi Scan Updated", "QRIS Validated", PayMethod.QRIS, 20000L)
        val updatedSale = db.sDao.getSale(saleId)
        assertEquals(PayMethod.QRIS, updatedSale?.payMethod)
        assertEquals("QRIS Validated", updatedSale?.note)
    }
}

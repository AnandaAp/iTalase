package com.anlian.italase.shared.ui.state

import com.anlian.italase.shared.data.local.entity.DebtEntity
import com.anlian.italase.shared.data.local.entity.DebtStatus
import com.anlian.italase.shared.data.local.entity.PayMethod
import com.anlian.italase.shared.data.local.entity.ProductEntity
import com.anlian.italase.shared.data.repository.CartLine
import com.anlian.italase.shared.data.repository.FakeDatabase
import com.anlian.italase.shared.data.repository.ShopRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.datetime.LocalDate
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class ShopViewModelsTest {

    private val testDispatcher = UnconfinedTestDispatcher()

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createEnv(): Pair<FakeDatabase, ShopRepository> {
        val db = FakeDatabase()
        val repo = ShopRepository(db)
        return db to repo
    }

    // =========================================================================
    // ProductViewModel Tests
    // =========================================================================

    @Test
    fun testProductViewModelSearchAndCategory() = runTest {
        val (_, repo) = createEnv()
        val vm = ProductViewModel(repo)

        vm.search("kopi")
        assertEquals("kopi", vm.query.value)

        vm.selectCategory("Minuman")
        assertEquals("Minuman", vm.category.value)

        vm.selectCategory(null)
        assertNull(vm.category.value)
    }

    @Test
    fun testProductViewModelSaveValidations() = runTest {
        val (_, repo) = createEnv()
        val vm = ProductViewModel(repo)
        backgroundScope.launch { vm.products.collect() }
        backgroundScope.launch { vm.visibleProducts.collect() }

        // 1. Empty name
        vm.save(name = "", buy = 1000, sell = 2000, stock = 10, unit = "pcs", barcode = "", category = "Makanan")
        assertEquals(UiMessage.NameRequired, vm.message.first { it != null })
        vm.clearMessage()

        // 2. Sell price <= 0
        vm.save(name = "Kopi", buy = 1000, sell = 0, stock = 10, unit = "pcs", barcode = "", category = "Minuman")
        assertEquals(UiMessage.PricePositive, vm.message.first { it != null })
        vm.clearMessage()

        vm.save(name = "Kopi", buy = 1000, sell = -500, stock = 10, unit = "pcs", barcode = "", category = "Minuman")
        assertEquals(UiMessage.PricePositive, vm.message.first { it != null })
        vm.clearMessage()

        // 3. Valid save
        vm.save(name = "Kopi", buy = 3000, sell = 5000, stock = 10, unit = "pcs", barcode = "123", category = "Minuman")
        assertEquals(UiMessage.ProductSaved, vm.message.first { it != null })
        vm.clearMessage()

        // 4. Delete product
        val p = ProductEntity(id = 1L, name = "Kopi", buyPrice = 3000, sellPrice = 5000, stock = 10)
        vm.delete(p)
        assertEquals(UiMessage.ProductDeleted, vm.message.first { it != null })
    }

    // =========================================================================
    // CashierViewModel Tests
    // =========================================================================

    @Test
    fun testCashierViewModelCartOperations() = runTest {
        val (db, repo) = createEnv()
        val p1 = ProductEntity(id = 1L, name = "Biskuit", buyPrice = 4000, sellPrice = 6000, stock = 5)
        val p2 = ProductEntity(id = 2L, name = "Air Mineral", buyPrice = 2000, sellPrice = 3000, stock = 2)
        db.pDao.upsert(p1)
        db.pDao.upsert(p2)

        val vm = CashierViewModel(repo)
        backgroundScope.launch { vm.products.collect() }
        vm.products.first { it.isNotEmpty() }
        advanceUntilIdle()

        // Search & category
        vm.search("air")
        assertEquals("air", vm.query.value)
        vm.selectCategory("Minuman")
        assertEquals("Minuman", vm.category.value)

        // Add p1
        vm.add(p1)
        assertEquals(1, vm.cart.value[1L])
        assertEquals(6000L, vm.cartTotal())

        // Add p1 again
        vm.add(p1)
        assertEquals(2, vm.cart.value[1L])
        assertEquals(12000L, vm.cartTotal())

        // Set qty directly
        vm.setQty(1L, 4)
        assertEquals(4, vm.cart.value[1L])

        // Add exceeding stock
        vm.setQty(2L, 5) // stock is 2
        assertTrue(vm.message.value is UiMessage.StockShort)
        assertNull(vm.cart.value[2L])

        // Set qty <= 0 removes from cart
        vm.setQty(1L, 0)
        assertFalse(vm.cart.value.containsKey(1L))

        // Re-add and clearCart
        vm.add(p1)
        assertEquals(1, vm.cartLines().size)
        vm.clearCart()
        assertTrue(vm.cart.value.isEmpty())
        assertNull(vm.lastSaleId.value)
    }

    @Test
    fun testCashierViewModelCheckout() = runTest {
        val (db, repo) = createEnv()
        val p = ProductEntity(id = 1L, name = "Ciki", buyPrice = 1000, sellPrice = 2000, stock = 10)
        db.pDao.upsert(p)

        val vm = CashierViewModel(repo)
        backgroundScope.launch { vm.products.collect() }
        vm.products.first { it.isNotEmpty() }
        advanceUntilIdle()

        vm.add(p)

        // Checkout success
        vm.checkout(paid = 5000, method = PayMethod.TUNAI, customer = "Doni")
        val msg = vm.message.first { it != null }
        assertNotNull(vm.lastSaleId.value)
        assertTrue(msg is UiMessage.TxnSaved)
        assertTrue(vm.cart.value.isEmpty())
        vm.clearMessage()

        // Checkout empty cart throws CartEmpty
        vm.checkout(paid = 1000, method = PayMethod.TUNAI, customer = "Doni")
        val emptyMsg = vm.message.first { it != null }
        assertEquals(UiMessage.CartEmpty, emptyMsg)
    }

    // =========================================================================
    // DashboardViewModel Tests
    // =========================================================================

    @Test
    fun testDashboardViewModelRefresh() = runTest {
        val (db, repo) = createEnv()
        val p = ProductEntity(id = 1L, name = "Roti", buyPrice = 2000, sellPrice = 5000, stock = 10)
        db.pDao.upsert(p)

        val vm = DashboardViewModel(repo)
        backgroundScope.launch { vm.products.collect() }
        backgroundScope.launch { vm.activeDebts.collect() }

        vm.refresh()
        advanceUntilIdle()

        assertNull(vm.error.value)
        assertEquals(0L, vm.summary.value.omzet)
        assertEquals(0L, vm.piutang.value)
    }

    // =========================================================================
    // DebtViewModel Tests
    // =========================================================================

    @Test
    fun testDebtViewModelOperations() = runTest {
        val (_, repo) = createEnv()
        val vm = DebtViewModel(repo)
        backgroundScope.launch { vm.debts.collect() }

        // 1. Manual debt validation failure
        vm.saveManual(customer = "", amount = 10000, note = "Kosong")
        assertEquals(UiMessage.CustomerRequired, vm.message.first { it != null })
        vm.clearMessage()

        // 2. Manual debt valid
        vm.saveManual(customer = "Budi", amount = 50000, note = "Pinjam")
        assertEquals(UiMessage.DebtSaved, vm.message.first { it != null })
        vm.clearMessage()

        // 3. Pay debt
        vm.pay(id = 1L, amount = 20000)
        val msg = vm.message.first { it != null }
        assertTrue(msg is UiMessage.DebtPaid)
        assertEquals(20000L, (msg as UiMessage.DebtPaid).amount)
        vm.clearMessage()

        // 4. Pay missing debt
        vm.pay(id = 9999L, amount = 10000)
        assertEquals(UiMessage.DebtMissing, vm.message.first { it != null })
    }

    // =========================================================================
    // ExpenseViewModel Tests
    // =========================================================================

    @Test
    fun testExpenseViewModelOperations() = runTest {
        val (_, repo) = createEnv()
        val vm = ExpenseViewModel(repo)
        backgroundScope.launch { vm.items.collect() }

        // Validation failure
        vm.save(name = "", category = "Ops", amount = 10000, note = "")
        assertEquals(UiMessage.ExpenseNameRequired, vm.message.first { it != null })
        vm.clearMessage()

        // Valid save
        vm.save(name = "Sapu", category = "Ops", amount = 15000, note = "")
        assertEquals(UiMessage.ExpenseSaved, vm.message.first { it != null })
        vm.clearMessage()

        // Delete
        vm.delete(1L)
        advanceUntilIdle()

        // Load more
        vm.loadMore()
    }

    // =========================================================================
    // HistoryViewModel Tests
    // =========================================================================

    @Test
    fun testHistoryViewModelOperations() = runTest {
        val (db, repo) = createEnv()
        val p = ProductEntity(id = 1L, name = "Kopi", buyPrice = 2000, sellPrice = 4000, stock = 10)
        db.pDao.upsert(p)
        val saleId = repo.checkout(listOf(CartLine(p, 1)), 5000, PayMethod.TUNAI, "Andi", "Catatan")

        val vm = HistoryViewModel(repo)
        backgroundScope.launch { vm.sales.collect() }
        backgroundScope.launch { vm.categories.collect() }

        // Category filter
        vm.selectCategory("Minuman")
        assertEquals("Minuman", vm.category.value)
        vm.selectCategory("Minuman") // No-op branch

        // Range filter: check automatic swap when from > to
        val d1 = LocalDate(2026, 10, 25)
        val d2 = LocalDate(2026, 10, 20)
        vm.selectRange(d1, d2)
        assertEquals(d2, vm.rangeFrom.value)
        assertEquals(d1, vm.rangeTo.value)

        // Select last days & clear range
        vm.selectLastDays(7)
        assertNotNull(vm.rangeFrom.value)
        vm.clearRange()
        assertNull(vm.rangeFrom.value)

        // Load more
        vm.loadMore()

        // Select & load sale detail
        vm.selectSale(saleId)
        assertEquals(saleId, vm.selectedId.value)

        vm.loadDetail(saleId)
        advanceUntilIdle()
        assertEquals("Andi", vm.detail.value?.sale?.customerName)

        // Update info with customer and note
        vm.updateInfo(saleId, "Andi Pratama", "Diantar")
        assertEquals(UiMessage.SaleInfoSaved, vm.detailMessage.first { it != null })
        assertEquals("Andi Pratama", vm.detail.value?.sale?.customerName)

        // Update info with payment method and tendered
        vm.updateInfo(saleId, "Andi Pratama", "Diantar transfer", PayMethod.TRANSFER, 4000L)
        advanceUntilIdle()
        assertEquals(PayMethod.TRANSFER, vm.detail.value?.sale?.payMethod)
        assertEquals(4000L, vm.detail.value?.sale?.tendered)

        // Export PDF
        vm.exportPdf()
        advanceUntilIdle()
        assertNotNull(vm.exportMessage.value)
        vm.clearExportMessage()
        assertNull(vm.exportMessage.value)
    }

    @Test
    fun testCashierViewModelCheckoutBon() = runTest {
        val (db, repo) = createEnv()
        val p = ProductEntity(id = 1L, name = "Beras 5kg", buyPrice = 50000, sellPrice = 68000, stock = 5)
        db.pDao.upsert(p)

        val vm = CashierViewModel(repo)
        backgroundScope.launch { vm.products.collect() }
        vm.products.first { it.isNotEmpty() }
        advanceUntilIdle()

        vm.add(p)
        assertEquals(68000L, vm.cartTotal())

        // Checkout with BON
        vm.checkout(paid = 0, method = PayMethod.BON, customer = "Pak RT")
        advanceUntilIdle()

        val msg = vm.message.first { it != null }
        assertTrue(msg is UiMessage.TxnSaved)
        assertTrue(vm.cart.value.isEmpty())

        // Verify debt created in repository
        val debts = repo.observeActiveDebts().first()
        assertTrue(debts.any { it.customerName == "Pak RT" && it.remaining == 68000L })
    }
}

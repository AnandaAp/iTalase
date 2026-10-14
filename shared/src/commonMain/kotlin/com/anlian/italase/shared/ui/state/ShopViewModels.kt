package com.anlian.italase.shared.ui.state

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.anlian.italase.shared.data.ProductCategories
import com.anlian.italase.shared.data.ShopDefaults
import com.anlian.italase.shared.data.local.dao.TopRow
import com.anlian.italase.shared.data.local.entity.DebtEntity
import com.anlian.italase.shared.data.local.entity.ExpenseEntity
import com.anlian.italase.shared.data.local.entity.ProductEntity
import com.anlian.italase.shared.data.local.entity.SaleEntity
import com.anlian.italase.shared.data.repository.CartLine
import com.anlian.italase.shared.data.repository.DaySummary
import com.anlian.italase.shared.data.repository.ShopException
import com.anlian.italase.shared.data.repository.ShopRepository
import com.anlian.italase.shared.util.filterProducts
import com.anlian.italase.shared.util.nowMillis
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.koin.core.annotation.Singleton

private const val SUBSCRIBE_TIMEOUT_MILLIS = 5_000L

@Singleton
class DashboardViewModel(private val repo: ShopRepository) : ViewModel() {
    val products: StateFlow<List<ProductEntity>> =
        repo.observeProducts().stateIn(viewModelScope, SharingStarted.WhileSubscribed(SUBSCRIBE_TIMEOUT_MILLIS), emptyList())

    val activeDebts: StateFlow<List<DebtEntity>> =
        repo.observeActiveDebts().stateIn(viewModelScope, SharingStarted.WhileSubscribed(SUBSCRIBE_TIMEOUT_MILLIS), emptyList())

    val summary: MutableStateFlow<DaySummary> = MutableStateFlow(DaySummary())
    val top: MutableStateFlow<List<TopRow>> = MutableStateFlow(emptyList())
    val piutang: MutableStateFlow<Long> = MutableStateFlow(0)
    val error: MutableStateFlow<UiMessage?> = MutableStateFlow(null)

    fun refresh() {
        viewModelScope.launch {
            try {
                summary.value = repo.daySummary(nowMillis())
                top.value = repo.topProducts(nowMillis())
                piutang.value = repo.totalPiutang()
                error.value = null
            } catch (t: Throwable) {
                error.value = UiMessage.SummaryFailed
            }
        }
    }
}

@Singleton
class ProductViewModel(private val repo: ShopRepository) : ViewModel() {
    val products: StateFlow<List<ProductEntity>> =
        repo.observeProducts().stateIn(viewModelScope, SharingStarted.WhileSubscribed(SUBSCRIBE_TIMEOUT_MILLIS), emptyList())
    val message: MutableStateFlow<UiMessage?> = MutableStateFlow(null)
    val query: MutableStateFlow<String> = MutableStateFlow("")
    val category: MutableStateFlow<String?> = MutableStateFlow(null)

    val categories: StateFlow<List<String>> = products
        .map { list -> list.map { it.category.ifBlank { ProductCategories.OTHER } }.distinct().sorted() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(SUBSCRIBE_TIMEOUT_MILLIS), emptyList())

    val visibleProducts: StateFlow<List<ProductEntity>> =
        combine(products, query, category) { list, q, c -> filterProducts(list, q, c) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(SUBSCRIBE_TIMEOUT_MILLIS), emptyList())

    fun search(q: String) {
        query.value = q
    }

    fun selectCategory(c: String?) {
        category.value = c
    }

    fun save(
        name: String,
        buy: Long,
        sell: Long,
        stock: Int,
        unit: String,
        barcode: String,
        category: String,
        editId: Long = 0,
    ) {
        viewModelScope.launch {
            try {
                if (name.isBlank()) {
                    message.value = UiMessage.NameRequired
                    return@launch
                }
                if (sell <= 0) {
                    message.value = UiMessage.PricePositive
                    return@launch
                }
                repo.saveProduct(
                    ProductEntity(
                        id = editId, name = name.trim(), barcode = barcode.trim(),
                        category = category.trim().ifBlank { ProductCategories.OTHER },
                        buyPrice = buy.coerceAtLeast(0), sellPrice = sell,
                        stock = stock.coerceAtLeast(0), unit = unit.ifBlank { ShopDefaults.UNIT },
                    ),
                )
                message.value = UiMessage.ProductSaved
            } catch (t: Throwable) {
                message.value = UiMessage.SaveFailed
            }
        }
    }

    fun delete(p: ProductEntity) {
        viewModelScope.launch {
            try {
                repo.deleteProduct(p)
                message.value = UiMessage.ProductDeleted
            } catch (t: Throwable) {
                message.value = UiMessage.DeleteFailed
            }
        }
    }

    fun clearMessage() {
        message.value = null
    }
}

@Singleton
class CashierViewModel(private val repo: ShopRepository) : ViewModel() {
    val products: StateFlow<List<ProductEntity>> =
        repo.observeProducts().stateIn(viewModelScope, SharingStarted.WhileSubscribed(SUBSCRIBE_TIMEOUT_MILLIS), emptyList())

    val cart: MutableStateFlow<Map<Long, Int>> = MutableStateFlow(emptyMap())
    val query: MutableStateFlow<String> = MutableStateFlow("")
    val category: MutableStateFlow<String?> = MutableStateFlow(null)
    val message: MutableStateFlow<UiMessage?> = MutableStateFlow(null)
    val lastSaleId: MutableStateFlow<Long?> = MutableStateFlow(null)

    val categories: StateFlow<List<String>> = products
        .map { list -> list.map { it.category.ifBlank { ProductCategories.OTHER } }.distinct().sorted() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(SUBSCRIBE_TIMEOUT_MILLIS), emptyList())

    val visibleProducts: StateFlow<List<ProductEntity>> =
        combine(products, query, category) { list, q, c -> filterProducts(list, q, c) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(SUBSCRIBE_TIMEOUT_MILLIS), emptyList())

    fun search(q: String) {
        query.value = q
        message.value = null
    }

    fun selectCategory(c: String?) {
        category.value = c
    }

    fun add(p: ProductEntity) {
        val cur: MutableMap<Long, Int> = cart.value.toMutableMap()
        val qty: Int = (cur[p.id] ?: 0) + 1
        if (qty > p.stock) {
            message.value = UiMessage.StockShort(p.name)
            return
        }
        cur[p.id] = qty
        cart.value = cur
        message.value = null
    }

    fun setQty(id: Long, qty: Int) {
        val cur: MutableMap<Long, Int> = cart.value.toMutableMap()
        val product = products.value.firstOrNull { it.id == id }
        if (product != null && qty > product.stock) {
            message.value = UiMessage.StockShort(product.name)
            return
        }
        if (qty <= 0) cur.remove(id) else cur[id] = qty
        cart.value = cur
        message.value = null
    }

    fun clearCart() {
        cart.value = emptyMap()
        lastSaleId.value = null
        message.value = null
    }

    fun cartLines(): List<CartLine> {
        val byId: Map<Long, ProductEntity> = products.value.associateBy { it.id }
        return cart.value.mapNotNull { (id, qty) -> byId[id]?.let { CartLine(it, qty) } }
    }

    fun cartTotal(): Long = cartLines().sumOf { it.product.sellPrice * it.qty.toLong() }

    private var checkoutBusy = false

    fun checkout(paid: Long, method: String, customer: String) {
        if (checkoutBusy) return
        checkoutBusy = true
        viewModelScope.launch {
            try {
                val id: Long = repo.checkout(cartLines(), paid, method, customer)
                lastSaleId.value = id
                cart.value = emptyMap()
                message.value = UiMessage.TxnSaved(id)
            } catch (e: ShopException) {
                message.value = e.code.toMessage(e.arg)
            } catch (t: Throwable) {
                message.value = UiMessage.CheckoutFailed
            } finally {
                checkoutBusy = false
            }
        }
    }

    fun clearMessage() {
        message.value = null
    }
}

@Singleton
@OptIn(ExperimentalCoroutinesApi::class)
class HistoryViewModel(private val repo: ShopRepository) : ViewModel() {
    val category: MutableStateFlow<String?> = MutableStateFlow(null)

    val sales: StateFlow<List<SaleEntity>> = category.flatMapLatest { c ->
        if (c == null) repo.observeRecentSales(ShopRepository.RECENT_SALES_LIMIT)
        else repo.observeSalesByCategory(c, ShopRepository.RECENT_SALES_LIMIT)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(SUBSCRIBE_TIMEOUT_MILLIS), emptyList())

    val categories: StateFlow<List<String>> = repo.observeItemCategories()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(SUBSCRIBE_TIMEOUT_MILLIS), emptyList())

    fun selectCategory(c: String?) {
        category.value = c
    }

    val detail: MutableStateFlow<ShopRepository.SaleDetail?> = MutableStateFlow(null)
    val detailMessage: MutableStateFlow<UiMessage?> = MutableStateFlow(null)
    val selectedId: MutableStateFlow<Long> = MutableStateFlow(0L)

    fun selectSale(id: Long) {
        selectedId.value = id
    }

    fun loadDetail(id: Long) {
        detailMessage.value = null
        viewModelScope.launch {
            detail.value = repo.saleDetail(id)
        }
    }

    fun updateInfo(id: Long, customer: String, note: String) {
        viewModelScope.launch {
            try {
                repo.updateSaleInfo(id, customer, note)
                detail.value = repo.saleDetail(id)
                detailMessage.value = UiMessage.SaleInfoSaved
            } catch (t: Throwable) {
                detailMessage.value = UiMessage.GenericFailure
            }
        }
    }
}

@Singleton
class DebtViewModel(private val repo: ShopRepository) : ViewModel() {
    val debts: StateFlow<List<DebtEntity>> =
        repo.observeDebts().stateIn(viewModelScope, SharingStarted.WhileSubscribed(SUBSCRIBE_TIMEOUT_MILLIS), emptyList())
    val message: MutableStateFlow<UiMessage?> = MutableStateFlow(null)
    private var payBusy = false

    fun saveManual(customer: String, amount: Long, note: String) {
        viewModelScope.launch {
            try {
                repo.saveManualDebt(customer, amount, note)
                message.value = UiMessage.DebtSaved
            } catch (e: ShopException) {
                message.value = e.code.toMessage(e.arg)
            } catch (t: Throwable) {
                message.value = UiMessage.DebtSaveFailed
            }
        }
    }

    fun pay(id: Long, amount: Long) {
        if (payBusy) return
        payBusy = true
        viewModelScope.launch {
            try {
                val recorded: Long = repo.payDebt(id, amount)
                message.value = UiMessage.DebtPaid(recorded)
            } catch (e: ShopException) {
                message.value = e.code.toMessage(e.arg)
            } catch (t: Throwable) {
                message.value = UiMessage.DebtPayFailed
            } finally {
                payBusy = false
            }
        }
    }

    fun clearMessage() {
        message.value = null
    }
}

@Singleton
class ExpenseViewModel(private val repo: ShopRepository) : ViewModel() {
    val items: StateFlow<List<ExpenseEntity>> =
        repo.observeExpenses().stateIn(viewModelScope, SharingStarted.WhileSubscribed(SUBSCRIBE_TIMEOUT_MILLIS), emptyList())
    val message: MutableStateFlow<UiMessage?> = MutableStateFlow(null)

    fun save(name: String, category: String, amount: Long, note: String) {
        viewModelScope.launch {
            try {
                repo.saveExpense(name, category, amount, note)
                message.value = UiMessage.ExpenseSaved
            } catch (e: ShopException) {
                message.value = e.code.toMessage(e.arg)
            } catch (t: Throwable) {
                message.value = UiMessage.ExpenseSaveFailed
            }
        }
    }

    fun delete(id: Long) {
        viewModelScope.launch {
            repo.deleteExpense(id)
        }
    }

    fun clearMessage() {
        message.value = null
    }
}

package com.anlian.italase.shared.data.repository

import androidx.room.InvalidationTracker
import androidx.room.RoomDatabase
import androidx.room.RoomOpenDelegate
import com.anlian.italase.shared.data.local.ITalaseDatabase
import com.anlian.italase.shared.data.local.dao.DebtDao
import com.anlian.italase.shared.data.local.dao.ExpenseDao
import com.anlian.italase.shared.data.local.dao.ProductDao
import com.anlian.italase.shared.data.local.dao.SaleDao
import com.anlian.italase.shared.data.local.dao.TopRow
import com.anlian.italase.shared.data.local.entity.DebtEntity
import com.anlian.italase.shared.data.local.entity.DebtPaymentEntity
import com.anlian.italase.shared.data.local.entity.DebtStatus
import com.anlian.italase.shared.data.local.entity.ExpenseEntity
import com.anlian.italase.shared.data.local.entity.ProductEntity
import com.anlian.italase.shared.data.local.entity.SaleEntity
import com.anlian.italase.shared.data.local.entity.SaleItemEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.lang.reflect.InvocationHandler
import java.lang.reflect.Method
import java.lang.reflect.Proxy
import kotlin.coroutines.Continuation

open class FakeDatabase(
    val pDao: InMemoryProductDao = InMemoryProductDao(),
    val sDao: InMemorySaleDao = InMemorySaleDao(),
    val dDao: InMemoryDebtDao = InMemoryDebtDao(),
    val eDao: InMemoryExpenseDao = InMemoryExpenseDao(),
) : ITalaseDatabase() {
    init {
        setupFakeConnectionManager()
    }

    override fun productDao(): ProductDao = pDao
    override fun saleDao(): SaleDao = sDao
    override fun debtDao(): DebtDao = dDao
    override fun expenseDao(): ExpenseDao = eDao
    override fun createOpenDelegate(): RoomOpenDelegate = throw NotImplementedError()
    override fun createInvalidationTracker(): InvalidationTracker = throw NotImplementedError()
    override fun clearAllTables() {}

    private fun setupFakeConnectionManager() {
        try {
            val unsafeClass = Class.forName("sun.misc.Unsafe")
            val theUnsafeField = unsafeClass.getDeclaredField("theUnsafe")
            theUnsafeField.isAccessible = true
            val unsafe = theUnsafeField.get(null)
            val allocateInstanceMethod = unsafeClass.getMethod("allocateInstance", Class::class.java)

            val cmClass = Class.forName("androidx.room.RoomConnectionManager")
            val cmInstance = allocateInstanceMethod.invoke(unsafe, cmClass)

            val poolClass = Class.forName("androidx.room.coroutines.ConnectionPool")
            val transactorClass = Class.forName("androidx.room.Transactor")
            val scopeClass = Class.forName("androidx.room.TransactionScope")

            val scopeProxy = Proxy.newProxyInstance(
                scopeClass.classLoader,
                arrayOf(scopeClass),
            ) { _, _, _ -> null }

            val transactorProxy = Proxy.newProxyInstance(
                transactorClass.classLoader,
                arrayOf(transactorClass),
                object : InvocationHandler {
                    override fun invoke(proxy: Any?, method: Method, args: Array<out Any?>?): Any? {
                        return when (method.name) {
                            "inTransaction" -> true
                            "withTransaction" -> {
                                val block = args?.get(1)
                                @Suppress("UNCHECKED_CAST")
                                val continuation = args?.get(2) as? Continuation<Any?>
                                if (block != null && continuation != null) {
                                    val invokeMethod = block.javaClass.methods.first { it.name == "invoke" }
                                    invokeMethod.isAccessible = true
                                    try {
                                        invokeMethod.invoke(block, scopeProxy, continuation)
                                    } catch (e: java.lang.reflect.InvocationTargetException) {
                                        val target = e.targetException ?: e
                                        if (target is RuntimeException) throw target
                                        if (target is Error) throw target
                                        throw RuntimeException(target)
                                    }
                                } else null
                            }
                            else -> null
                        }
                    }
                },
            )

            val poolProxy = Proxy.newProxyInstance(
                poolClass.classLoader,
                arrayOf(poolClass),
                object : InvocationHandler {
                    override fun invoke(proxy: Any?, method: Method, args: Array<out Any?>?): Any? {
                        if (method.name == "useConnection") {
                            val block = args?.get(1)
                            @Suppress("UNCHECKED_CAST")
                            val continuation = args?.get(2) as? Continuation<Any?>
                            if (block != null && continuation != null) {
                                val invokeMethod = block.javaClass.methods.first { it.name == "invoke" }
                                invokeMethod.isAccessible = true
                                try {
                                    return invokeMethod.invoke(block, transactorProxy, continuation)
                                } catch (e: java.lang.reflect.InvocationTargetException) {
                                    val target = e.targetException ?: e
                                    if (target is RuntimeException) throw target
                                    if (target is Error) throw target
                                    throw RuntimeException(target)
                                }
                            }
                        }
                        return null
                    }
                },
            )

            var currClass: Class<*>? = cmClass
            var poolField: java.lang.reflect.Field? = null
            while (currClass != null && poolField == null) {
                try {
                    poolField = currClass.getDeclaredField("connectionPool")
                } catch (e: NoSuchFieldException) {
                    currClass = currClass.superclass
                }
            }
            poolField?.isAccessible = true
            poolField?.set(cmInstance, poolProxy)

            val cmField = RoomDatabase::class.java.getDeclaredField("connectionManager")
            cmField.isAccessible = true
            cmField.set(this, cmInstance)

            val scopeField = RoomDatabase::class.java.getDeclaredField("coroutineScope")
            scopeField.isAccessible = true
            scopeField.set(this, kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.SupervisorJob() + kotlinx.coroutines.Dispatchers.Unconfined))

            @Suppress("DEPRECATION")
            val tracker = InvalidationTracker(this, "products", "sales", "sale_items", "debts", "debt_payments", "expenses")
            val trackerField = RoomDatabase::class.java.getDeclaredField("internalTracker")
            trackerField.isAccessible = true
            trackerField.set(this, tracker)
        } catch (t: Throwable) {
            println("FakeConnectionManager warning: $t")
        }
    }
}

class InMemoryProductDao : ProductDao {
    val items = MutableStateFlow<List<ProductEntity>>(emptyList())
    var nextId = 1L

    override fun observeAll() = items.asStateFlow()
    override fun observeLowStock() = items.asStateFlow()
    override suspend fun getById(id: Long) = items.value.find { it.id == id }
    override suspend fun count() = items.value.size

    override suspend fun upsert(item: ProductEntity): Long {
        val id = if (item.id == 0L) nextId++ else item.id
        val newItem = item.copy(id = id)
        items.value = items.value.filter { it.id != id } + newItem
        return id
    }

    override suspend fun update(item: ProductEntity) {
        items.value = items.value.map { if (it.id == item.id) item else it }
    }

    override suspend fun delete(item: ProductEntity) {
        items.value = items.value.filter { it.id != item.id }
    }
}

class InMemorySaleDao : SaleDao {
    val sales = MutableStateFlow<List<SaleEntity>>(emptyList())
    val saleItems = mutableListOf<SaleItemEntity>()
    var nextSaleId = 1L

    override fun observeRecent(limit: Int) = sales.asStateFlow()
    override fun observeBetween(from: Long, to: Long, limit: Int) = sales.asStateFlow()
    override fun observeBetweenByCategory(from: Long, to: Long, category: String, limit: Int) = sales.asStateFlow()
    override fun observeSalesByCategory(category: String, limit: Int) = sales.asStateFlow()
    override fun observeItemCategories() = MutableStateFlow(listOf("Makanan", "Minuman")).asStateFlow()

    override suspend fun listRange(from: Long, to: Long) = sales.value.filter { it.dateMillis in from..to }
    override suspend fun countRange(from: Long, to: Long) = listRange(from, to).size
    override suspend fun sumOmzet(from: Long, to: Long) = listRange(from, to).sumOf { it.total }
    override suspend fun sumProfit(from: Long, to: Long) = listRange(from, to).sumOf { it.profit }
    override suspend fun dayAggregate(from: Long, to: Long): com.anlian.italase.shared.data.local.dao.DayAggregateRow {
        val list = listRange(from, to)
        return com.anlian.italase.shared.data.local.dao.DayAggregateRow(
            omzet = list.sumOf { it.total },
            profit = list.sumOf { it.profit },
            trxCount = list.size,
        )
    }
    override suspend fun getSale(id: Long) = sales.value.find { it.id == id }

    override suspend fun insertSale(sale: SaleEntity): Long {
        val id = nextSaleId++
        val newSale = sale.copy(id = id)
        sales.value = sales.value + newSale
        return id
    }

    override suspend fun updateInfo(id: Long, customer: String, note: String) {
        sales.value = sales.value.map {
            if (it.id == id) it.copy(customerName = customer, note = note) else it
        }
    }

    override suspend fun updatePaymentInfo(
        id: Long,
        customer: String,
        note: String,
        payMethod: String,
        paidAmount: Long,
        tendered: Long,
        status: String,
    ) {
        sales.value = sales.value.map {
            if (it.id == id) {
                it.copy(
                    customerName = customer,
                    note = note,
                    payMethod = payMethod,
                    paidAmount = paidAmount,
                    tendered = tendered,
                    status = status,
                )
            } else it
        }
    }

    override suspend fun insertItems(items: List<SaleItemEntity>) {
        saleItems.addAll(items)
    }

    override suspend fun itemsOf(saleId: Long) = saleItems.filter { it.saleId == saleId }
    override suspend fun topProducts(from: Long, to: Long): List<TopRow> {
        val inRangeSales = listRange(from, to).map { it.id }.toSet()
        val items = saleItems.filter { it.saleId in inRangeSales }
        return items.groupBy { it.productName }.map { (name, list) ->
            TopRow(name, list.sumOf { it.qty }, list.sumOf { it.subtotal })
        }.sortedByDescending { it.q }
    }
}

class InMemoryDebtDao : DebtDao {
    val debts = MutableStateFlow<List<DebtEntity>>(emptyList())
    val payments = mutableListOf<DebtPaymentEntity>()
    var nextDebtId = 1L
    var nextPayId = 1L

    override fun observeAll() = debts.asStateFlow()
    override fun observeActive() = debts.asStateFlow()
    override suspend fun getById(id: Long) = debts.value.find { it.id == id }
    override suspend fun getBySaleId(saleId: Long) = debts.value.find { it.saleId == saleId }
    override suspend fun insert(debt: DebtEntity): Long {
        val id = nextDebtId++
        val newDebt = debt.copy(id = id)
        debts.value = debts.value + newDebt
        return id
    }
    override suspend fun update(debt: DebtEntity) {
        debts.value = debts.value.map { if (it.id == debt.id) debt else it }
    }
    override suspend fun totalPiutang() = debts.value.filter { it.status == DebtStatus.BELUM_LUNAS }.sumOf { it.remaining }
    override suspend fun insertPayment(p: DebtPaymentEntity): Long {
        val id = nextPayId++
        payments.add(p.copy(id = id))
        return id
    }
    override suspend fun delete(id: Long) {
        debts.value = debts.value.filter { it.id != id }
    }
}

class InMemoryExpenseDao : ExpenseDao {
    val expenses = MutableStateFlow<List<ExpenseEntity>>(emptyList())
    var nextId = 1L

    override fun observePage(limit: Int) = expenses.asStateFlow()
    override suspend fun listRange(from: Long, to: Long) = expenses.value.filter { it.dateMillis in from..to }
    override suspend fun sumRange(from: Long, to: Long) = listRange(from, to).sumOf { it.amount }
    override suspend fun upsert(e: ExpenseEntity): Long {
        val id = if (e.id == 0L) nextId++ else e.id
        val newExpense = e.copy(id = id)
        expenses.value = expenses.value.filter { it.id != id } + newExpense
        return id
    }
    override suspend fun delete(id: Long) {
        expenses.value = expenses.value.filter { it.id != id }
    }
}

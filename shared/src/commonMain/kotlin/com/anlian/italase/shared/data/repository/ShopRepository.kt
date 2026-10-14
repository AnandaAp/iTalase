package com.anlian.italase.shared.data.repository

import androidx.room.immediateTransaction
import androidx.room.useWriterConnection
import com.anlian.italase.shared.data.ShopDefaults
import com.anlian.italase.shared.data.local.ITalaseDatabase
import com.anlian.italase.shared.data.local.dao.TopRow
import com.anlian.italase.shared.data.local.entity.DebtEntity
import com.anlian.italase.shared.data.local.entity.DebtPaymentEntity
import com.anlian.italase.shared.data.local.entity.DebtStatus
import com.anlian.italase.shared.data.local.entity.ExpenseEntity
import com.anlian.italase.shared.data.local.entity.PayMethod
import com.anlian.italase.shared.data.local.entity.ProductEntity
import com.anlian.italase.shared.data.local.entity.SaleEntity
import com.anlian.italase.shared.data.local.entity.SaleItemEntity
import com.anlian.italase.shared.data.local.entity.TxnStatus
import com.anlian.italase.shared.util.endOfLocalDayMillis
import com.anlian.italase.shared.util.startOfLocalDayMillis
import com.anlian.italase.shared.util.nowMillis
import kotlinx.coroutines.flow.Flow
import org.koin.core.annotation.Singleton

data class CartLine(
    val product: ProductEntity,
    val qty: Int,
)

data class DaySummary(
    val omzet: Long = 0,
    val profit: Long = 0,
    val trxCount: Int = 0,
    val expense: Long = 0,
    val bersih: Long = 0,
)

@Singleton
class ShopRepository(
    private val db: ITalaseDatabase,
) {
    companion object {
        const val RECENT_SALES_LIMIT = 200
    }
    // ---- Produk ----
    fun observeProducts(): Flow<List<ProductEntity>> = db.productDao().observeAll()

    fun observeLowStock(): Flow<List<ProductEntity>> = db.productDao().observeLowStock()

    suspend fun searchProducts(q: String): List<ProductEntity> =
        if (q.isBlank()) db.productDao().search("") else db.productDao().search(q.trim())

    suspend fun saveProduct(p: ProductEntity): Long {
        val now: Long = nowMillis()
        val existing: ProductEntity? = if (p.id == 0L) null else db.productDao().getById(p.id)
        val row: ProductEntity = if (existing == null) {
            p.copy(id = 0, createdAt = now, updatedAt = now)
        } else {
            p.copy(createdAt = existing.createdAt, updatedAt = now)
        }
        return db.productDao().upsert(row)
    }

    suspend fun deleteProduct(p: ProductEntity) = db.productDao().delete(p)

    suspend fun adjustStock(productId: Long, delta: Int) {
        val cur: ProductEntity = db.productDao().getById(productId) ?: return
        db.productDao().update(cur.copy(stock = (cur.stock + delta).coerceAtLeast(0), updatedAt = nowMillis()))
    }

    // ---- Kasir / checkout (offline, satu transaksi database atomik) ----
    suspend fun checkout(
        lines: List<CartLine>,
        paidAmount: Long,
        payMethod: String,
        customerName: String,
        note: String = "",
    ): Long {
        if (lines.isEmpty()) throw ShopException(ErrorCode.EMPTY_CART)
        var saleId = -1L
        db.useWriterConnection { transactor ->
            transactor.immediateTransaction {
                saleId = checkoutLocked(lines, paidAmount, payMethod, customerName, note)
            }
        }
        return saleId
    }

    private suspend fun checkoutLocked(
        lines: List<CartLine>,
        paidAmount: Long,
        payMethod: String,
        customerName: String,
        note: String,
    ): Long {
        val now: Long = nowMillis()
        var total = 0L
        var profit = 0L
        val items: MutableList<SaleItemEntity> = mutableListOf()
        for (line in lines) {
            val fresh: ProductEntity = db.productDao().getById(line.product.id)
                ?: throw ShopException(ErrorCode.PRODUCT_GONE, line.product.name)
            if (line.qty <= 0) throw ShopException(ErrorCode.QTY_INVALID)
            if (fresh.stock < line.qty) throw ShopException(ErrorCode.STOCK_SHORT, fresh.name)
            val sub: Long = fresh.sellPrice * line.qty.toLong()
            val pr: Long = (fresh.sellPrice - fresh.buyPrice) * line.qty.toLong()
            total += sub
            profit += pr
            items.add(
                SaleItemEntity(
                    saleId = 0, productId = fresh.id, productName = fresh.name,
                    category = fresh.category,
                    qty = line.qty, buyPrice = fresh.buyPrice, sellPrice = fresh.sellPrice,
                    subtotal = sub, profit = pr,
                ),
            )
        }
        val isBon: Boolean = payMethod == PayMethod.BON || paidAmount < total
        val status: String = if (isBon) TxnStatus.BON else TxnStatus.LUNAS
        val saleId: Long = db.saleDao().insertSale(
            SaleEntity(
                dateMillis = now, total = total, profit = profit,
                payMethod = if (isBon) PayMethod.BON else payMethod,
                paidAmount = if (isBon) paidAmount.coerceAtLeast(0) else total,
                tendered = paidAmount.coerceAtLeast(0),
                customerName = customerName.trim(), status = status, note = note.trim(),
            ),
        )
        db.saleDao().insertItems(items.map { it.copy(saleId = saleId) })
        for (line in lines) {
            val cur: ProductEntity = db.productDao().getById(line.product.id) ?: continue
            db.productDao().update(cur.copy(stock = (cur.stock - line.qty).coerceAtLeast(0), updatedAt = now))
        }
        if (isBon) {
            val sisa: Long = (total - paidAmount.coerceAtLeast(0)).coerceAtLeast(0)
            if (sisa > 0) {
                db.debtDao().insert(
                    DebtEntity(
                        customerName = customerName.ifBlank { ShopDefaults.CUSTOMER },
                        saleId = saleId, totalAmount = total, paidAmount = paidAmount.coerceAtLeast(0),
                        remaining = sisa,
                        status = DebtStatus.BELUM_LUNAS,
                        dateMillis = now, note = "",
                    ),
                )
            }
        }
        return saleId
    }

    // ---- Riwayat & laporan ----
    fun observeRecentSales(limit: Int = RECENT_SALES_LIMIT): Flow<List<SaleEntity>> =
        db.saleDao().observeRecent(limit)

    fun observeSalesByCategory(category: String, limit: Int = RECENT_SALES_LIMIT): Flow<List<SaleEntity>> =
        db.saleDao().observeSalesByCategory(category, limit)

    fun observeItemCategories(): Flow<List<String>> = db.saleDao().observeItemCategories()

    data class SaleDetail(
        val sale: SaleEntity,
        val items: List<SaleItemEntity>,
    )

    suspend fun saleDetail(id: Long): SaleDetail? {
        val sale: SaleEntity = db.saleDao().getSale(id) ?: return null
        return SaleDetail(sale, db.saleDao().itemsOf(id))
    }

    suspend fun updateSaleInfo(id: Long, customer: String, note: String) {
        db.saleDao().updateInfo(id, customer.trim(), note.trim())
    }

    suspend fun saleItems(saleId: Long): List<SaleItemEntity> = db.saleDao().itemsOf(saleId)

    suspend fun daySummary(millis: Long): DaySummary {
        val from: Long = startOfLocalDayMillis(millis)
        val to: Long = endOfLocalDayMillis(millis)
        val omzet: Long = db.saleDao().sumOmzet(from, to)
        val profit: Long = db.saleDao().sumProfit(from, to)
        val count: Int = db.saleDao().listRange(from, to).size
        val expense: Long = db.expenseDao().sumRange(from, to)
        return DaySummary(omzet = omzet, profit = profit, trxCount = count, expense = expense, bersih = profit - expense)
    }

    suspend fun topProducts(millis: Long): List<TopRow> {
        val from: Long = startOfLocalDayMillis(millis)
        val to: Long = endOfLocalDayMillis(millis)
        return db.saleDao().topProducts(from, to)
    }

    // ---- Hutang / Bon ----
    fun observeDebts(): Flow<List<DebtEntity>> = db.debtDao().observeAll()

    fun observeActiveDebts(): Flow<List<DebtEntity>> = db.debtDao().observeActive()

    suspend fun totalPiutang(): Long = db.debtDao().totalPiutang()

    suspend fun saveManualDebt(customer: String, amount: Long, note: String): Long {
        if (customer.isBlank()) throw ShopException(ErrorCode.CUSTOMER_REQUIRED)
        if (amount <= 0) throw ShopException(ErrorCode.AMOUNT_POSITIVE)
        return db.debtDao().insert(
            DebtEntity(
                customerName = customer.trim(), totalAmount = amount, paidAmount = 0,
                remaining = amount, status = DebtStatus.BELUM_LUNAS,
                dateMillis = nowMillis(), note = note.trim(),
            ),
        )
    }

    // Kelebihan bayar dibatasi sebesar sisa (dianggap kembalian),
    // mengembalikan nominal yang benar-benar tercatat.
    suspend fun payDebt(debtId: Long, amount: Long, note: String = ""): Long {
        if (amount <= 0) throw ShopException(ErrorCode.AMOUNT_POSITIVE)
        val d: DebtEntity = db.debtDao().getById(debtId) ?: throw ShopException(ErrorCode.DEBT_MISSING)
        if (d.remaining <= 0) throw ShopException(ErrorCode.ALREADY_PAID)
        val effective: Long = minOf(amount, d.remaining)
        val now: Long = nowMillis()
        db.debtDao().insertPayment(DebtPaymentEntity(debtId = debtId, amount = effective, dateMillis = now, note = note))
        val newPaid: Long = d.paidAmount + effective
        val newRem: Long = (d.totalAmount - newPaid).coerceAtLeast(0)
        db.debtDao().update(
            d.copy(
                paidAmount = newPaid, remaining = newRem,
                status = if (newRem == 0L) DebtStatus.LUNAS else DebtStatus.BELUM_LUNAS,
            ),
        )
        return effective
    }

    // ---- Pengeluaran ----
    fun observeExpenses(): Flow<List<ExpenseEntity>> = db.expenseDao().observeAll()

    suspend fun saveExpense(name: String, category: String, amount: Long, note: String): Long {
        if (name.isBlank()) throw ShopException(ErrorCode.EXPENSE_NAME_REQUIRED)
        if (amount <= 0) throw ShopException(ErrorCode.AMOUNT_POSITIVE)
        return db.expenseDao().upsert(
            ExpenseEntity(
                name = name.trim(), category = category.trim().ifBlank { ShopDefaults.CATEGORY },
                amount = amount, dateMillis = nowMillis(), note = note.trim(),
            ),
        )
    }

    suspend fun deleteExpense(id: Long) = db.expenseDao().delete(id)
}

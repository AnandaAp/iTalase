package com.anlian.italase.shared.data.repository

import androidx.compose.runtime.Immutable
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
import com.anlian.italase.shared.util.PAGE_SIZE
import com.anlian.italase.shared.util.endOfLocalDayMillis
import com.anlian.italase.shared.util.nowMillis
import com.anlian.italase.shared.util.startOfLocalDayMillis
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import org.koin.core.annotation.Singleton

/** Satu baris keranjang kasir. `product` adalah salinan yang dibawa UI agar
 *  total sementara bisa dihitung tanpa query; harga dan stok final dibaca ulang
 *  dari database saat checkout, jadi angka di salinan ini bukan kebenaran. */
@Immutable
data class CartLine(
    /** Produk yang dipilih; hanya [ProductEntity.id]-nya yang dipercaya saat checkout. */
    val product: ProductEntity,
    /** Jumlah yang diminta; harus > 0 dan tidak melebihi stok, divalidasi
     *  di dalam transaksi checkout. */
    val qty: Int,
)

/** Ringkasan satu hari untuk layar beranda. Semuanya dihitung ulang dari tabel
 *  saat dipanggil — tidak ada yang disimpan, jadi tidak bisa basi. */
@Immutable
data class DaySummary(
    /** Omzet kotor, termasuk bagian yang dibon. */
    val omzet: Long = 0,
    /** Margin penjualan hari itu, sebelum pengeluaran dikurangkan. */
    val profit: Long = 0,
    /** Jumlah struk, bukan jumlah baris barang. */
    val trxCount: Int = 0,
    /** Total pengeluaran hari itu. */
    val expense: Long = 0,
    /** profit - expense; negatif berarti hari itu rugi. */
    val bersih: Long = 0,
)

/** Satu-satunya pintu tulis ke database untuk fitur toko. Kegagalan yang bisa
 *  diperbaiki kasir dilempar sebagai [ShopException]; setiap perubahan yang
 *  menyentuh lebih dari satu tabel dijalankan dalam satu transaksi database.
 *
 *  Instance-nya singleton (Koin), jadi state yang perlu dibagi antar layar —
 *  seperti daftar produk — hidup di sini, bukan di masing-masing ViewModel. */
@Singleton
class ShopRepository(
    /** Database aplikasi; repository hanya memakai DAO-nya, bukan SQL langsung. */
    private val db: ITalaseDatabase,
) {
    companion object {
        /** Berapa lama cache daftar produk bertahan setelah pelanggan terakhir
         *  berhenti mengamati: cukup lama untuk berpindah tab bolak-balik tanpa
         *  query ulang, cukup pendek agar memori tidak menahan data lama. */
        private const val SUBSCRIBE_TIMEOUT_MILLIS = 5_000L
    }

    // Repository ini singleton seumur app, jadi di sinilah tempat satu-satunya
    // salinan daftar produk.
    //
    // Tanpa ini: Beranda, Kasir, dan Stok masing-masing memanggil
    // observeProducts() → Room menjalankan query yang SAMA 3x dan menyalin
    // SELURUH tabel 3x setiap kali tabelnya berubah (mis. tiap checkout
    // mengurangi stok). Dengan ini: 1 query, 1 daftar, 3 pelanggan. Ini juga
    // yang bikin jumlah baris tidak lagi mengalikan biaya perubahan stok.
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val productsCache: StateFlow<List<ProductEntity>> = db.productDao().observeAll()
        .stateIn(scope, SharingStarted.WhileSubscribed(SUBSCRIBE_TIMEOUT_MILLIS), emptyList())

    // ---- Produk ----
    /** Daftar produk untuk seluruh layar. Dijawab dari productsCache, jadi
     *  berapa pun pelanggannya tetap satu query ke Room. */
    fun observeProducts(): Flow<List<ProductEntity>> = productsCache

    /** Barang yang stoknya menipis; sengaja tanpa cache karena hanya dipakai
     *  layar stok, bukan layar lain yang selalu terbuka. */
    fun observeLowStock(): Flow<List<ProductEntity>> = db.productDao().observeLowStock()

    /** Menyimpan barang baru (id 0) atau perubahan barang lama. `createdAt`
     *  diambil dari baris yang sudah ada dan `updatedAt` selalu di-set ke
     *  sekarang, jadi kedua kolom itu tidak boleh dipercaya dari UI. */
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

    /** Menghapus barang beserta stoknya. Riwayat penjualan tetap utuh karena
     *  nama dan harga sudah tersalin ke baris struk. */
    suspend fun deleteProduct(p: ProductEntity) = db.productDao().delete(p)

    /** Menambah/mengurangi stok untuk koreksi manual. Hasilnya tidak pernah
     *  negatif, dan barang yang sudah dihapus diabaikan tanpa error. */
    suspend fun adjustStock(productId: Long, delta: Int) {
        val cur: ProductEntity = db.productDao().getById(productId) ?: return
        db.productDao().update(cur.copy(stock = (cur.stock + delta).coerceAtLeast(0), updatedAt = nowMillis()))
    }

    // ---- Kasir / checkout (offline, satu transaksi database atomik) ----
    /** Membukukan satu transaksi kasir: struk, rincian, pengurangan stok, dan
     *  bon (kalau ada sisa) berhasil atau gagal bersama — tidak ada struk yang
     *  tercatat tanpa stok yang terpotong.
     *
     *  Keranjang kosong melempar [ErrorCode.EMPTY_CART], validasi per baris
     *  melempar kode lain (mis. [ErrorCode.STOCK_SHORT]). Mengembalikan id struk.
     *  Kalau `paidAmount` kurang dari total, transaksi dicatat sebagai BON dan
     *  sisanya menjadi piutang. */
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

    /** Isi sebenarnya dari [checkout]. Harus dipanggil di dalam transaksi tulis
     *  (immediateTransaction) supaya stok yang dibaca tidak bisa berubah antara
     *  validasi dan pengurangan stok. */
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
    /** Struk terbaru untuk daftar riwayat; `limit` adalah jumlah baris yang
     *  diminta (lihat util/Paging.kt untuk cara bertambahnya). */
    /**
     * Riwayat penjualan untuk satu rentang waktu, dengan atau tanpa saringan
     * kategori — SATU pintu untuk keempat kombinasi, supaya ViewModel tidak
     * bercabang sendiri dan `LIMIT` yang bertumbuh tetap dipakai di semua jalur.
     *
     * `from`/`to` adalah epoch millis inklusif; kalau keduanya null berarti tanpa
     * batas (seluruh riwayat). Rentang dihitung pemanggil lewat
     * [com.anlian.italase.shared.util.dayRangeMillis].
     */
    fun observeSalesInRange(
        from: Long,
        to: Long,
        category: String?,
        limit: Int = PAGE_SIZE,
    ): Flow<List<SaleEntity>> = when {
        category.isNullOrBlank() -> db.saleDao().observeBetween(from, to, limit)
        else -> db.saleDao().observeBetweenByCategory(from, to, category, limit)
    }

    fun observeRecentSales(limit: Int = PAGE_SIZE): Flow<List<SaleEntity>> =
        db.saleDao().observeRecent(limit)

    /** Struk yang memuat minimal satu barang berkategori tersebut. */
    fun observeSalesByCategory(category: String, limit: Int = PAGE_SIZE): Flow<List<SaleEntity>> =
        db.saleDao().observeSalesByCategory(category, limit)

    /** Kategori yang benar-benar terpakai di struk, untuk mengisi pilihan
     *  filter riwayat. */
    fun observeItemCategories(): Flow<List<String>> = db.saleDao().observeItemCategories()

    /** Struk beserta rinciannya, dibaca sekaligus supaya layar detail tidak
     *  memanggil database dua kali. */
    data class SaleDetail(
        /** Header struk: total, metode bayar, status, dan nama pelanggannya. */
        val sale: SaleEntity,
        /** Baris barang milik struk tersebut. */
        val items: List<SaleItemEntity>,
    )

    /** Null kalau struknya tidak ada; pemanggil harus menangani kondisi kosong
     *  alih-alih mengasumsikan struk selalu ketemu. */
    suspend fun saleDetail(id: Long): SaleDetail? {
        val sale: SaleEntity = db.saleDao().getSale(id) ?: return null
        return SaleDetail(sale, db.saleDao().itemsOf(id))
    }

    /** Memperbaiki nama pelanggan dan catatan struk (mis. salah ketik). Angka
     *  transaksi dan stok sengaja tidak bisa diubah dari sini. */
    suspend fun updateSaleInfo(id: Long, customer: String, note: String) {
        db.saleDao().updateInfo(id, customer.trim(), note.trim())
    }

    /** Rincian satu struk saja, untuk layar yang sudah punya header struknya. */
    suspend fun saleItems(saleId: Long): List<SaleItemEntity> = db.saleDao().itemsOf(saleId)

    /** Ringkasan satu hari untuk beranda: omzet, profit, jumlah struk, dan
     *  pengeluaran, semuanya dihitung dari batas hari lokal. */
    suspend fun daySummary(millis: Long): DaySummary {
        val from: Long = startOfLocalDayMillis(millis)
        val to: Long = endOfLocalDayMillis(millis)
        val omzet: Long = db.saleDao().sumOmzet(from, to)
        val profit: Long = db.saleDao().sumProfit(from, to)
        val count: Int = db.saleDao().listRange(from, to).size
        val expense: Long = db.expenseDao().sumRange(from, to)
        return DaySummary(omzet = omzet, profit = profit, trxCount = count, expense = expense, bersih = profit - expense)
    }

    /** Barang terlaris pada hari `millis`; agregasinya dikerjakan di SQL. */
    suspend fun topProducts(millis: Long): List<TopRow> {
        val from: Long = startOfLocalDayMillis(millis)
        val to: Long = endOfLocalDayMillis(millis)
        return db.saleDao().topProducts(from, to)
    }

    // ---- Hutang / Bon ----
    /** Semua bon, aktif maupun yang sudah lunas; sumber layar riwayat bon. */
    fun observeDebts(): Flow<List<DebtEntity>> = db.debtDao().observeAll()

    /** Hanya bon yang masih belum lunas. */
    fun observeActiveDebts(): Flow<List<DebtEntity>> = db.debtDao().observeActive()

    /** Total sisa tagihan semua bon aktif, untuk angka ringkasan piutang. */
    suspend fun totalPiutang(): Long = db.debtDao().totalPiutang()

    /** Membuat bon tanpa penjualan (mis. pelanggan meminjam uang tunai).
     *  Nama pelanggan wajib diisi dan nominalnya harus > 0. */
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
    /** Satu halaman pengeluaran terbaru; `limit` tumbuh tiap muat-tambahan
     *  (lihat util/Paging.kt), nilai awalnya milik pemanggil. */
    fun observeExpenses(limit: Int = PAGE_SIZE): Flow<List<ExpenseEntity>> =
        db.expenseDao().observePage(limit)

    /** Mencatat pengeluaran baru. Nama wajib diisi, nominal > 0, dan kategori
     *  yang kosong diisi [ShopDefaults.CATEGORY] supaya laporan tetap rapi. */
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

    /** Hapus permanen satu baris pengeluaran; tidak ada undo di lapisan data. */
    suspend fun deleteExpense(id: Long) = db.expenseDao().delete(id)
}

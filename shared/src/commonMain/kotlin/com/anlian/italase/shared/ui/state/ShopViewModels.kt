package com.anlian.italase.shared.ui.state

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.anlian.italase.shared.data.ProductCategories
import com.anlian.italase.shared.data.ShopDefaults
import com.anlian.italase.shared.data.local.dao.TopRow
import com.anlian.italase.shared.data.local.entity.DebtEntity
import com.anlian.italase.shared.data.local.entity.ExpenseEntity
import com.anlian.italase.shared.data.local.entity.PayMethod
import com.anlian.italase.shared.data.local.entity.ProductEntity
import com.anlian.italase.shared.data.local.entity.SaleEntity
import com.anlian.italase.shared.data.local.entity.TxnStatus
import com.anlian.italase.shared.data.repository.CartLine
import com.anlian.italase.shared.data.repository.DaySummary
import com.anlian.italase.shared.data.repository.ShopException
import com.anlian.italase.shared.data.repository.ShopRepository
import com.anlian.italase.shared.report.EXPORT_LIMIT
import com.anlian.italase.shared.report.REPORT_APP_NAME
import com.anlian.italase.shared.report.REPORT_FOOTER
import com.anlian.italase.shared.report.REPORT_TAGLINE
import com.anlian.italase.shared.report.SalesReport
import com.anlian.italase.shared.report.SalesReportRow
import com.anlian.italase.shared.report.SalesReportTotals
import com.anlian.italase.shared.report.renderSalesReportPdf
import com.anlian.italase.shared.report.salesReportFileName
import com.anlian.italase.shared.report.savePdf
import com.anlian.italase.shared.ui.components.readAppLogo
import com.anlian.italase.shared.util.PAGE_SIZE
import com.anlian.italase.shared.util.ProductList
import com.anlian.italase.shared.util.dayRangeMillis
import com.anlian.italase.shared.util.formatDateRange
import com.anlian.italase.shared.util.hasMorePages
import com.anlian.italase.shared.util.localDateOf
import com.anlian.italase.shared.util.minusDays
import com.anlian.italase.shared.util.nowMillis
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate
import org.koin.core.annotation.Singleton

/**
 * Berapa lama aliran Room tetap hidup setelah pelanggan terakhir pergi.
 *
 * Terlalu pendek: query & observer diulang tiap perpindahan tab. Terlalu panjang: cursor
 * ditahan terus. Nilai ini yang membuat pindah tab tetap murah tanpa menahan resource
 * selamanya.
 */
private const val SUBSCRIBE_TIMEOUT_MILLIS = 5_000L

/**
 * Jeda sebelum query disaring. Mengetik "susu" itu 4 ketikan, tapi cuma butuh
 * 1 saring — dengan jeda ini yang jalan 1, bukan 4.
 */
private const val QUERY_DEBOUNCE_MILLIS = 120L

/**
 * Lantai waktu tampil indikator "memuat halaman berikutnya".
 *
 * SQLite lokal mengembalikan satu halaman dalam hitungan milidetik. Indikator
 * yang muncul lalu hilang dalam kurang dari satu frame cuma berkedip dan
 * terbaca seperti bug, jadi ditahan minimal selama ini. Ini BATAS TAMPIL
 * MINIMUM, bukan kerja palsu yang ditambahkan ke alur data.
 */
private const val MIN_LOADING_MILLIS = 400L

/**
 * Ringkasan hari ini untuk layar utama.
 *
 * Dua jenis data sengaja dipisah: [products] dan [activeDebts] mengalir reaktif dari Room,
 * sedangkan [summary], [top] dan [piutang] adalah agregat mahal yang hanya dihitung ulang
 * saat [refresh] dipanggil — bukan ikut berubah tiap baris transaksi.
 */
@Singleton
class DashboardViewModel(private val repo: ShopRepository) : ViewModel() {
    /**
     * Katalog lengkap, reaktif: berubah begitu ada produk disimpan atau dihapus.
     */
    val products: StateFlow<List<ProductEntity>> =
        repo.observeProducts().stateIn(viewModelScope, SharingStarted.WhileSubscribed(SUBSCRIBE_TIMEOUT_MILLIS), emptyList())

    /**
     * Hanya bon yang belum lunas; yang sudah lunas tidak ikut dibaca agar tidak ikut terhitung.
     */
    val activeDebts: StateFlow<List<DebtEntity>> =
        repo.observeActiveDebts().stateIn(viewModelScope, SharingStarted.WhileSubscribed(SUBSCRIBE_TIMEOUT_MILLIS), emptyList())

    /**
     * Agregat hari berjalan; awal hari masih kosong sampai [refresh] selesai.
     */
    val summary: MutableStateFlow<DaySummary> = MutableStateFlow(DaySummary())

    /**
     * Produk terlaris hari ini, hasil query agregat.
     */
    val top: MutableStateFlow<List<TopRow>> = MutableStateFlow(emptyList())

    /**
     * Total sisa piutang seluruh pelanggan (rupiah).
     */
    val piutang: MutableStateFlow<Long> = MutableStateFlow(0)

    /**
     * Jalan kegagalan agregasi sampai ke layar; aliran reaktif di atas tidak melewatinya.
     */
    val error: MutableStateFlow<UiMessage?> = MutableStateFlow(null)

    /**
     * Hitung ulang semua agregat dashboard.
     *
     * Aman dipanggil berulang; kegagalan tidak dilempar, hanya mengisi [error] — dashboard yang
     * stale lebih baik daripada layar yang mati.
     */
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

/**
 * Pengelolaan katalog untuk layar Stok: daftar, pencarian, simpan dan hapus.
 *
 * Semua masukan (kata kunci, kategori, form) bermuara ke [visibleProducts]; layar tidak
 * pernah menyaring sendiri.
 */
@OptIn(FlowPreview::class)
@Singleton
class ProductViewModel(private val repo: ShopRepository) : ViewModel() {
    /**
     * Sumber daftar, reaktif — satu-satunya penulisnya Room, ViewModel tidak menyentuhnya.
     */
    val products: StateFlow<List<ProductEntity>> =
        repo.observeProducts().stateIn(viewModelScope, SharingStarted.WhileSubscribed(SUBSCRIBE_TIMEOUT_MILLIS), emptyList())

    /**
     * Pesan hasil tulis (simpan/hapus) yang menunggu ditampilkan lalu [clearMessage].
     */
    val message: MutableStateFlow<UiMessage?> = MutableStateFlow(null)

    /**
     * Kata kunci mentah apa adanya; normalisasi & debounce baru terjadi saat daftar saring dihitung.
     */
    val query: MutableStateFlow<String> = MutableStateFlow("")

    /**
     * Kategori aktif; `null` berarti semua kategori.
     */
    val category: MutableStateFlow<String?> = MutableStateFlow(null)

    /**
     * Daftar terurut + kunci huruf-kecil, dihitung SEKALI per perubahan tabel.
     * Inilah pemisahnya: urutan & `lowercase()` tidak lagi diulang tiap ketikan.
     */
    private val productList: StateFlow<ProductList> = products
        .map { ProductList.of(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(SUBSCRIBE_TIMEOUT_MILLIS), ProductList.EMPTY)

    /**
     * Kategori yang benar-benar ada di tabel, unik dan terurut; kategori kosong dilaporkan
     * sebagai [ProductCategories.OTHER] supaya tidak ada chip tanpa nama.
     */
    val categories: StateFlow<List<String>> = products
        .map { list -> list.map { it.category.ifBlank { ProductCategories.OTHER } }.distinct().sorted() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(SUBSCRIBE_TIMEOUT_MILLIS), emptyList())

    /**
     * Hasil saring terakhir — satu-satunya daftar yang dirender layar.
     */
    val visibleProducts: StateFlow<List<ProductEntity>> =
        combine(productList, query.debounce(QUERY_DEBOUNCE_MILLIS), category) { list, q, c ->
            list.visible(q, c)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(SUBSCRIBE_TIMEOUT_MILLIS), emptyList())

    /**
     * Ganti kata kunci; daftar baru muncul setelah debounce, jadi ini bukan saring per ketikan.
     */
    fun search(q: String) {
        query.value = q
    }

    /**
     * Ganti kategori aktif; `null` berarti semua.
     */
    fun selectCategory(c: String?) {
        category.value = c
    }

    /**
     * Simpan atau timpa satu produk.
     *
     * Validasi nama & harga ada di sini supaya pesannya spesifik sebelum tabel disentuh; sisa
     * nilai dinormalkan (trim, lantai nol) supaya tidak ada harga negatif atau kategori kosong
     * yang tersimpan diam-diam.
     *
     * @param editId 0 = produk baru; selain itu id baris yang ditimpa.
     */
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

    /**
     * Hapus produk dari katalog.
     *
     * Baris penjualan lama tidak ikut berubah: penjualan menyimpan salinan nama & harga, jadi
     * riwayat tetap utuh walau produknya hilang.
     */
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

    /**
     * Bersihkan [message] setelah ditampilkan, agar tidak muncul lagi saat layar dibuka ulang.
     */
    fun clearMessage() {
        message.value = null
    }
}

/**
 * Layar Kasir: katalog + keranjang + checkout.
 *
 * Keranjang disimpan sebagai peta id → jumlah, bukan salinan baris produk: harga & stok
 * selalu diambil dari [products], jadi perubahan di tabel langsung terlihat tanpa
 * menggandakan data.
 */
@OptIn(FlowPreview::class)
@Singleton
class CashierViewModel(private val repo: ShopRepository) : ViewModel() {
    /**
     * Sumber daftar & stok; dipakai juga untuk menolak jumlah di atas stok dan menghitung total.
     */
    val products: StateFlow<List<ProductEntity>> =
        repo.observeProducts().stateIn(viewModelScope, SharingStarted.WhileSubscribed(SUBSCRIBE_TIMEOUT_MILLIS), emptyList())

    /**
     * Isi keranjang: id produk → jumlah. Produk yang sudah hilang dari tabel dibuang saat
     * dirender [cartLines].
     */
    val cart: MutableStateFlow<Map<Long, Int>> = MutableStateFlow(emptyMap())

    /**
     * Kata kunci mentah dari kolom cari kasir.
     */
    val query: MutableStateFlow<String> = MutableStateFlow("")

    /**
     * Kategori aktif di kasir; `null` berarti semua.
     */
    val category: MutableStateFlow<String?> = MutableStateFlow(null)

    /**
     * Pesan stok kurang atau hasil checkout; `null` berarti tidak ada yang perlu ditampilkan.
     */
    val message: MutableStateFlow<UiMessage?> = MutableStateFlow(null)

    /**
     * Id transaksi terakhir yang berhasil, untuk melanjutkan ke struk; direset oleh [clearCart].
     */
    val lastSaleId: MutableStateFlow<Long?> = MutableStateFlow(null)

    /**
     * Daftar terurut + kunci huruf-kecil, dihitung SEKALI per perubahan tabel.
     * Inilah pemisahnya: urutan & `lowercase()` tidak lagi diulang tiap ketikan.
     */
    private val productList: StateFlow<ProductList> = products
        .map { ProductList.of(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(SUBSCRIBE_TIMEOUT_MILLIS), ProductList.EMPTY)

    /**
     * Kategori yang ada di tabel untuk baris chip; kategori kosong dilaporkan sebagai
     * [ProductCategories.OTHER].
     */
    val categories: StateFlow<List<String>> = products
        .map { list -> list.map { it.category.ifBlank { ProductCategories.OTHER } }.distinct().sorted() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(SUBSCRIBE_TIMEOUT_MILLIS), emptyList())

    /**
     * Hasil saring katalog — satu-satunya daftar yang dirender.
     */
    val visibleProducts: StateFlow<List<ProductEntity>> =
        combine(productList, query.debounce(QUERY_DEBOUNCE_MILLIS), category) { list, q, c ->
            list.visible(q, c)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(SUBSCRIBE_TIMEOUT_MILLIS), emptyList())

    /**
     * Ganti kata kunci, sekaligus membuang pesan lama supaya tidak menempel di hasil baru.
     */
    fun search(q: String) {
        query.value = q
        message.value = null
    }

    /**
     * Ganti kategori aktif; `null` berarti semua.
     */
    fun selectCategory(c: String?) {
        category.value = c
    }

    /**
     * Tambah satu unit; ditolak (dengan pesan) kalau sudah menyentuh batas stok.
     */
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

    /**
     * Setel jumlah langsung dari input: nol atau kurang membuang barisnya, di atas stok ditolak.
     */
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

    /**
     * Kosongkan keranjang setelah transaksi selesai atau dibatalkan.
     */
    fun clearCart() {
        cart.value = emptyMap()
        lastSaleId.value = null
        message.value = null
    }

    /**
     * Baris keranjang siap dirender & dihitung: hanya produk yang masih ada di tabel.
     */
    fun cartLines(): List<CartLine> {
        val byId: Map<Long, ProductEntity> = products.value.associateBy { it.id }
        return cart.value.mapNotNull { (id, qty) -> byId[id]?.let { CartLine(it, qty) } }
    }

    /**
     * Total rupiah keranjang dihitung dari harga jual terkini, bukan harga saat barang
     * ditambahkan — harga yang berubah langsung ikut terhitung.
     */
    fun cartTotal(): Long = cartLines().sumOf { it.product.sellPrice * it.qty.toLong() }

    /**
     * Penjaga reentrancy: dialog bayar bisa memicu checkout dua kali sebelum coroutine selesai,
     * dan dua transaksi untuk satu pembayaran berarti stok terpotong dobel.
     */
    private var checkoutBusy = false

    /**
     * Catat penjualan lalu kosongkan keranjang.
     *
     * Validasi sisa stok yang sebenarnya ada di repository ([ShopException]): penolakan di [add]
     * cuma upaya terbaik, karena stok pasti baru diketahui saat transaksi ditulis.
     */
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

    /**
     * Bersihkan [message] setelah ditampilkan.
     */
    fun clearMessage() {
        message.value = null
    }
}

/**
 * Riwayat penjualan dengan paging LIMIT bertumbuh (`hasMorePages`) dan filter kategori.
 *
 * Filter & limit digabung lewat `flatMapLatest` supaya mengganti kategori membuang permintaan
 * halaman lama, bukan menumpuknya di belakang daftar baru.
 */
@Singleton
@OptIn(ExperimentalCoroutinesApi::class)
class HistoryViewModel(private val repo: ShopRepository) : ViewModel() {
    /**
     * Kategori terpilih; `null` berarti semua. Ubah lewat [selectCategory] agar limit ikut direset.
     */
    val category: MutableStateFlow<String?> = MutableStateFlow(null)

    /** Batas baris yang diminta ke SQL — tumbuh PAGE_SIZE tiap muat-tambahan. */
    private val limit: MutableStateFlow<Int> = MutableStateFlow(PAGE_SIZE)

    /**
     * true selama [MIN_LOADING_MILLIS] setelah [loadMore] — menahan indikator agar tidak berkedip.
     */
    private val loading: MutableStateFlow<Boolean> = MutableStateFlow(false)

    /**
     * Rentang tanggal yang dipilih di layar; `null` di kedua sisi = seluruh riwayat.
     *
     * Disimpan sebagai [LocalDate] (bukan millis) karena UI yang mengubahnya lewat
     * pemilih tanggal, dan batas hari harus dihitung ulang menurut zona perangkat
     * setiap kali query dijalankan.
     */
    val rangeFrom: MutableStateFlow<LocalDate?> = MutableStateFlow(null)
    val rangeTo: MutableStateFlow<LocalDate?> = MutableStateFlow(null)

    /**
     * Baris yang dirender sejauh ini; jumlahnya mengikuti limit yang sedang diminta.
     *
     * Keempat kombinasi (kategori × rentang) diserahkan ke repository lewat satu
     * pintu [ShopRepository.observeSalesInRange], jadi tidak ada cabang ganda di sini.
     */
    val sales: StateFlow<List<SaleEntity>> =
        combine(category, limit, rangeFrom, rangeTo) { c, l, from, to -> Filter(c, l, from, to) }
            .flatMapLatest { f ->
                val (start, end) = dayRangeMillis(f.from, f.to)
                repo.observeSalesInRange(start, end, f.category, f.limit)
            }
            .stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(SUBSCRIBE_TIMEOUT_MILLIS),
                emptyList()
            )

    /** Pasangan filter yang sedang aktif; kelas kecil supaya `combine` 4 argumen tetap terbaca. */
    private data class Filter(
        val category: String?,
        val limit: Int,
        val from: LocalDate?,
        val to: LocalDate?,
    )

    /**
     * Kategori dari item penjualan yang pernah tercatat, bukan kategori produk aktif — ini yang
     * menjelaskan isi riwayat, bukan isi katalog hari ini.
     */
    val categories: StateFlow<List<String>> = repo.observeItemCategories()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(SUBSCRIBE_TIMEOUT_MILLIS), emptyList())

    /**
     * Salinan baca-saja dari [loading] untuk layar.
     */
    val loadingMore: StateFlow<Boolean> = loading

    /**
     * Masih ada halaman berikutnya? Dihitung dari jumlah baris vs limit yang diminta.
     */
    val hasMore: StateFlow<Boolean> = combine(sales, limit) { s, l -> hasMorePages(s.size, l) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(SUBSCRIBE_TIMEOUT_MILLIS), false)

    /** Satu halaman lagi, tepat PAGE_SIZE. Diabaikan kalau sudah mentok/masih memuat. */
    fun loadMore() {
        if (loading.value || !hasMore.value) return
        loading.value = true
        limit.value += PAGE_SIZE
        viewModelScope.launch {
            delay(MIN_LOADING_MILLIS)
            loading.value = false
        }
    }

    /**
     * Ganti filter kategori; limit dikembalikan ke halaman pertama supaya tidak ada halaman
     * kategori lama yang ikut terbawa.
     */
    fun selectCategory(c: String?) {
        if (category.value == c) return
        category.value = c
        resetPaging()
    }

    /**
     * Ubah rentang tanggal. `null` di kedua sisi berarti seluruh riwayat.
     *
     * Rentang yang terbalik (dari > sampai) ditukar otomatis, bukan ditolak: user
     * yang menekan "sampai" lebih dulu tidak perlu dipermalukan dengan error.
     */
    fun selectRange(from: LocalDate?, to: LocalDate?) {
        val (f, s) = if (from != null && to != null && from > to) to to from else from to to
        if (rangeFrom.value == f && rangeTo.value == s) return
        rangeFrom.value = f
        rangeTo.value = s
        resetPaging()
    }

    /** Pintasan rentang: `days` hari terakhir sampai hari ini. */
    fun selectLastDays(days: Int) {
        val today: LocalDate = localDateOf(nowMillis())
        selectRange(today.minusDays(days - 1), today)
    }

    /** Hapus rentang (kembali ke seluruh riwayat). */
    fun clearRange() = selectRange(null, null)

    /** Ganti saringan = mulai dari halaman pertama lagi. */
    private fun resetPaging() {
        limit.value = PAGE_SIZE
        loading.value = false
    }

    /**
     * Seluruh baris yang cocok dengan filter aktif, untuk diekspor ke PDF.
     *
     * Sengaja TIDAK memakai [sales]: itu hanya halaman yang sudah dimuat, jadi
     * laporan akan diam-diam terpotong sebanyak yang belum di-scroll. Rentang tetap
     * dihormati karena user mengharapkan "yang sedang saya lihat".
     */
    suspend fun loadAllForExport(): List<SaleEntity> {
        val (start, end) = dayRangeMillis(rangeFrom.value, rangeTo.value)
        return repo.observeSalesInRange(start, end, category.value, limit = EXPORT_LIMIT)
            .first()
    }

    /** true selama berkas PDF sedang disiapkan, supaya tombol ekspor bisa dikunci. */
    private val exportingState: MutableStateFlow<Boolean> = MutableStateFlow(false)
    val exporting: StateFlow<Boolean> = exportingState

    /** Hasil ekspor terakhir; `null` = belum ada. Layar menampilkan pesannya. */
    val exportMessage: MutableStateFlow<UiMessage?> = MutableStateFlow(null)

    /** Bersihkan pesan ekspor setelah ditampilkan, agar tidak menempel di layar. */
    fun clearExportMessage() {
        exportMessage.value = null
    }

    /**
     * Bentuk dan simpan laporan PDF untuk filter yang sedang aktif.
     *
     * Alurnya sengaja di ViewModel, bukan di layar: laporan butuh seluruh rentang
     * (bukan hanya halaman yang ter-scroll), ditambah label bahasa Indonesia yang
     * bergantung pada locale dan kategori aktif. Layar cukup memanggil satu aksi.
     */
    fun exportPdf() {
        if (exportingState.value) return
        exportingState.value = true
        exportMessage.value = null
        viewModelScope.launch {
            try {
                val rows: List<SaleEntity> = loadAllForExport()
                val logo: ByteArray? = runCatching { readAppLogo() }.getOrNull()
                val report = SalesReport(
                    appName = REPORT_APP_NAME,
                    tagline = REPORT_TAGLINE,
                    title = "Laporan Riwayat Transaksi",
                    periodLabel = formatDateRange(rangeFrom.value, rangeTo.value),
                    filterLabel = category.value?.let { "Kategori: $it" } ?: "Kategori: Semua",
                    generatedAtMillis = nowMillis(),
                    rows = rows.map { s ->
                        SalesReportRow(
                            receiptNo = s.id,
                            dateMillis = s.dateMillis,
                            methodLabel = payMethodLabelOf(s.payMethod),
                            statusLabel = if (s.status == TxnStatus.LUNAS) "Lunas" else "Bon",
                            paid = s.status == TxnStatus.LUNAS,
                            total = s.total,
                            profit = s.profit,
                            customerName = s.customerName,
                        )
                    },
                    totals = SalesReportTotals(
                        count = rows.size,
                        revenue = rows.sumOf { it.total },
                        profit = rows.sumOf { it.profit },
                        unpaid = rows.filter { it.status != TxnStatus.LUNAS }
                            .sumOf { it.total - it.paidAmount },
                    ),
                    footerNote = REPORT_FOOTER,
                    invoiceLimitReached = rows.size >= EXPORT_LIMIT,
                )
                val pdf: ByteArray = renderSalesReportPdf(report, logo)
                val saved: Boolean = savePdf(salesReportFileName(report), pdf)
                exportMessage.value =
                    if (saved) UiMessage.ReportExported else UiMessage.ReportExportFailed
            } catch (t: Throwable) {
                exportMessage.value = UiMessage.ReportExportFailed
            } finally {
                exportingState.value = false
            }
        }
    }

    /**
     * Label metode bayar tanpa Compose.
     *
     * Di layar label ini datang dari `stringResource`, tetapi ViewModel tidak punya
     * akses ke resource Compose. Teksnya harus sama dengan yang tampil di daftar,
     * jadi nilainya diambil dari konstanta yang sama (`PayMethod`), dengan nilai
     * cadangan untuk data lama yang metodenya tidak dikenal.
     */
    private fun payMethodLabelOf(method: String): String = when (method) {
        PayMethod.TUNAI -> "Tunai"
        PayMethod.TRANSFER -> "Transfer"
        else -> "Bon"
    }

    /**
     * Struk yang sedang dibuka; `null` berarti belum ada yang dimuat.
     */
    val detail: MutableStateFlow<ShopRepository.SaleDetail?> = MutableStateFlow(null)

    /**
     * Hasil edit info struk (berhasil/gagal), khusus layar detail.
     */
    val detailMessage: MutableStateFlow<UiMessage?> = MutableStateFlow(null)

    /**
     * Id struk yang sedang dibuka; 0 berarti belum ada.
     */
    val selectedId: MutableStateFlow<Long> = MutableStateFlow(0L)

    /**
     * Tandai struk yang dibuka; pemuatan isinya dilakukan [loadDetail].
     */
    fun selectSale(id: Long) {
        selectedId.value = id
    }

    /**
     * Ambil detail terbaru dari database; pesan edit sebelumnya dibuang agar tidak terbawa ke
     * struk lain.
     */
    fun loadDetail(id: Long) {
        detailMessage.value = null
        viewModelScope.launch {
            detail.value = repo.saleDetail(id)
        }
    }

    /**
     * Simpan nama pelanggan/catatan struk lalu muat ulang detailnya, supaya yang tampil
     * benar-benar isi database dan bukan tebakan layar.
     */
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

/**
 * Piutang: daftar bon dan pelunasan.
 *
 * Pelunasan dijaga [payBusy] karena dialog bayar bisa mengirim dua kali; dua pelunasan atas
 * satu bon berarti saldo pelanggan salah.
 */
@Singleton
class DebtViewModel(private val repo: ShopRepository) : ViewModel() {
    /**
     * Semua bon, lunas maupun belum; pemisahan tampilan dilakukan di layar.
     */
    val debts: StateFlow<List<DebtEntity>> =
        repo.observeDebts().stateIn(viewModelScope, SharingStarted.WhileSubscribed(SUBSCRIBE_TIMEOUT_MILLIS), emptyList())

    /**
     * Hasil simpan atau pelunasan terakhir, untuk snackbar.
     */
    val message: MutableStateFlow<UiMessage?> = MutableStateFlow(null)

    /**
     * Penjaga ganda-kirim tombol bayar; pelunasan ganda merusak saldo.
     */
    private var payBusy = false

    /**
     * Catat bon manual di luar transaksi kasir; validasi nama & nominal ada di repository.
     */
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

    /**
     * Catat pelunasan sebesar [amount].
     *
     * Nominal yang benar-benar tercatat datang dari repository, dan itulah yang dilaporkan ke
     * user — bukan angka yang diketik, supaya pesan selalu cocok dengan isi tabel.
     */
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

    /**
     * Bersihkan [message] setelah ditampilkan.
     */
    fun clearMessage() {
        message.value = null
    }
}

/**
 * Pengeluaran: daftar dengan paging LIMIT bertumbuh + form simpan.
 *
 * Pola [limit]/[loading] sama dengan riwayat penjualan; [loadingMore] sengaja tidak diekspos
 * sebagai MutableStateFlow supaya layar tidak bisa menghentikan indikator di tengah jalan.
 */
@Singleton
@OptIn(ExperimentalCoroutinesApi::class)
class ExpenseViewModel(private val repo: ShopRepository) : ViewModel() {
    /** Batas baris yang diminta ke SQL — tumbuh PAGE_SIZE tiap muat-tambahan. */
    private val limit: MutableStateFlow<Int> = MutableStateFlow(PAGE_SIZE)

    /**
     * true selama [MIN_LOADING_MILLIS] setelah [loadMore] (lihat catatan MIN_LOADING_MILLIS).
     */
    private val loading: MutableStateFlow<Boolean> = MutableStateFlow(false)

    /**
     * Baris pengeluaran yang dirender; jumlahnya mengikuti limit yang diminta.
     */
    val items: StateFlow<List<ExpenseEntity>> = limit
        .flatMapLatest { repo.observeExpenses(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(SUBSCRIBE_TIMEOUT_MILLIS), emptyList())

    /**
     * Salinan baca-saja dari [loading] untuk layar.
     */
    val loadingMore: StateFlow<Boolean> = loading

    /**
     * Masih ada halaman berikutnya? Bandingkan jumlah baris dengan limit yang diminta.
     */
    val hasMore: StateFlow<Boolean> = combine(items, limit) { i, l -> hasMorePages(i.size, l) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(SUBSCRIBE_TIMEOUT_MILLIS), false)

    /** Satu halaman lagi, tepat PAGE_SIZE. Diabaikan kalau sudah mentok/masih memuat. */
    fun loadMore() {
        if (loading.value || !hasMore.value) return
        loading.value = true
        limit.value += PAGE_SIZE
        viewModelScope.launch {
            // Lihat catatan MIN_LOADING_MILLIS.
            delay(MIN_LOADING_MILLIS)
            loading.value = false
        }
    }

    /**
     * Hasil operasi tulis (simpan) untuk snackbar.
     */
    val message: MutableStateFlow<UiMessage?> = MutableStateFlow(null)

    /**
     * Catat pengeluaran; validasi nama & nominal positif ada di repository.
     */
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

    /**
     * Hapus pengeluaran.
     *
     * Tidak ada pesan hasil: hilangnya baris dari daftar (reaktif) sudah jadi umpan baliknya.
     */
    fun delete(id: Long) {
        viewModelScope.launch {
            repo.deleteExpense(id)
        }
    }

    /**
     * Bersihkan [message] setelah ditampilkan.
     */
    fun clearMessage() {
        message.value = null
    }
}

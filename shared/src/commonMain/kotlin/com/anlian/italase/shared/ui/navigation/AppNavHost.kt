package com.anlian.italase.shared.ui.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.anlian.italase.shared.data.StockRules
import com.anlian.italase.shared.data.local.entity.ProductEntity
import com.anlian.italase.shared.platform.rememberAppExit
import com.anlian.italase.shared.ui.components.AppIcon
import com.anlian.italase.shared.ui.components.GlassDialogBackdrop
import com.anlian.italase.shared.ui.components.glassSurface
import com.anlian.italase.shared.ui.components.painter
import com.anlian.italase.shared.ui.screens.CashierScreen
import com.anlian.italase.shared.ui.screens.DashboardScreen
import com.anlian.italase.shared.ui.screens.DebtScreen
import com.anlian.italase.shared.ui.screens.ExpenseScreen
import com.anlian.italase.shared.ui.screens.HistoryScreen
import com.anlian.italase.shared.ui.screens.MoreScreen
import com.anlian.italase.shared.ui.screens.ProductScreen
import com.anlian.italase.shared.ui.screens.SaleDetailScreen
import com.anlian.italase.shared.ui.state.CashierViewModel
import com.anlian.italase.shared.ui.state.DashboardViewModel
import com.anlian.italase.shared.ui.state.DebtViewModel
import com.anlian.italase.shared.ui.state.ExpenseViewModel
import com.anlian.italase.shared.ui.state.HistoryViewModel
import com.anlian.italase.shared.ui.state.ProductViewModel
import com.anlian.italase.shared.ui.state.text
import italase.shared.generated.resources.Res
import italase.shared.generated.resources.cd_back
import italase.shared.generated.resources.tab_cashier
import italase.shared.generated.resources.tab_debt
import italase.shared.generated.resources.tab_home
import italase.shared.generated.resources.tab_more
import italase.shared.generated.resources.tab_stock
import kotlinx.collections.immutable.toImmutableList
import kotlinx.collections.immutable.toImmutableMap
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject

/**
 * Satu entri bottom nav: tujuan navigasi, label terlokalisasi, dan ikonnya.
 * Privat karena daftar tab tidak pernah dibentuk dari luar berkas ini.
 */
private data class Tab(val route: String, val label: StringResource, val icon: AppIcon)

/**
 * Susunan tab dari kiri ke kanan — urutan di sini adalah urutan render.
 * Route yang ditambahkan di sini juga harus masuk [Routes.TAB_ROUTES]; kalau
 * tidak, [AppNavHost] mem-push-nya sebagai sub-screen (back stack rusak dan
 * bottom bar jadi no-op di layar itu).
 */
private val TABS = listOf(
    Tab(Routes.HOME, Res.string.tab_home, AppIcon.HOME),
    Tab(Routes.CASHIER, Res.string.tab_cashier, AppIcon.SHOPPING_CART),
    Tab(Routes.STOCK, Res.string.tab_stock, AppIcon.INVENTORY),
    Tab(Routes.DEBT, Res.string.tab_debt, AppIcon.HANDSHAKE),
    Tab(Routes.MORE, Res.string.tab_more, AppIcon.MORE),
)

// Info top bar kini datang dari layar aktif (TopBarInfoEffect), jadi tabel
// judul-per-route di sini tidak diperlukan lagi — ia justru penyebab duplikasi.

// CompositionLocal to provide top app bar scroll behavior to descendant screens
val LocalTopAppBarScrollBehavior = compositionLocalOf<TopAppBarScrollBehavior?> { null }

// Top offset (status bar height + overlay app bar height) so screen content
// starts below the translucent overlay bar but scrolls underneath it (YouTube-style).
val LocalTopOverlayPadding = compositionLocalOf { 64.dp }

// Bottom offset (tab bar height + its margin + system nav bar) so scrollable
// screens can add trailing padding and scroll their last row ABOVE the floating
// glass bar instead of leaving it permanently hidden underneath.
val LocalBottomOverlayPadding = compositionLocalOf { 0.dp }

// Jarak elemen aksi mengambang (FAB, kartu checkout) dari tepi atas tab bar.
// SATU angka untuk semua layar, dipakai lewat [aboveTabBar] — supaya Stok, Bon,
// Pengeluaran, Detail Struk, dan Kasir punya jarak yang identik. Kalau salah
// satu layar pakai angka sendiri, ia akan terlihat "nyeleneh" sendiri.
val ACTION_OVER_BAR_GAP = 16.dp

// Tinggi ExtendedFloatingActionButton material3 yang dipakai layar-layar ini.
// Perlu diketahui karena container scroll di layar ber-FAB harus menyediakan
// ruang ekor setinggi itu, kalau tidak baris terakhir tertutup permanen.
val ACTION_FAB_HEIGHT = 56.dp

/**
 * Padding untuk SEMUA elemen aksi yang mengambang di atas tab bar.
 *
 * Pakai ini, jangan hitung sendiri. Layout yang benar: elemen di-`align` ke
 * BottomCenter/BottomEnd di dalam Box(fillMaxSize), lalu diberi modifier ini —
 * BUKAN ditaruh sebagai anak terakhir di dalam Column, karena Column akan
 * MEMESAN ruangnya dan mendorong elemen ke atas (itu yang bikin konten tidak
 * bisa lewat di belakang tab bar).
 */
@Composable
fun Modifier.aboveTabBar(side: Dp = 16.dp, gap: Dp = ACTION_OVER_BAR_GAP): Modifier =
    padding(start = side, end = side, bottom = LocalBottomOverlayPadding.current + gap)

/**
 * Padding ekor untuk container scroll di layar yang punya elemen aksi
 * mengambang, supaya baris terakhir bisa digulir ke ATAS elemen itu alih-alih
 * berhenti tertutup di bawahnya.
 *
 * [ACTION_OVER_BAR_GAP] dihitung DUA KALI, dan itu bukan kekeliruan: satu jarak
 * dipakai di BAWAH elemen aksi (antara elemen dan tab bar), satu lagi di
 * ATASNYA (antara elemen dan konten). Kalau hanya sekali — rumus sebelumnya —
 * baris terakhir berhenti TEPAT di tepi atas elemen aksi, jaraknya 0dp, dan
 * hasilnya terlihat menempel. Terukur: FAB atas di `overlay+96`, ekor lama
 * `overlay+96` → 0dp; ekor sekarang `overlay+112` → 16dp.
 *
 * @param actionHeight tinggi elemen aksinya: ACTION_FAB_HEIGHT untuk FAB,
 *   CHECKOUT_CARD_HEIGHT untuk kartu checkout Kasir.
 */
@Composable
fun bottomActionTailPadding(actionHeight: Dp): PaddingValues =
    PaddingValues(
        bottom = LocalBottomOverlayPadding.current +
            actionHeight +
            ACTION_OVER_BAR_GAP + // di bawah elemen aksi (elemen -> tab bar)
            ACTION_OVER_BAR_GAP, // di atas elemen aksi (elemen -> konten)
    )

// Tinggi konten top bar (di bawah status bar), dihitung dari isinya:
// 10 top + 16 labelSmall + 2 gap + 32 headlineSmall + 14 bottom = 74dp.
// Nilai ini harus PRESISI: kalau lebih besar, akan muncul celah kosong besar
// antara judul dan konten pertama.
private val TOP_BAR_CONTENT_HEIGHT = 74.dp

/**
 * State top bar, DIPISAH PER ROUTE.
 *
 * Kenapa per-route dan bukan satu holder global: saat navigasi, dua destinasi
 * hidup bersamaan selama transisi NavHost. Dengan satu holder, penulis terakhir
 * menang — jadi judul layar lama nyangkut di layar baru (bug "pindah cepat").
 * Dengan peta per-route, urutan penulisan tidak lagi menentukan: top bar selalu
 * membaca milik route yang sedang aktif.
 *
 * `behaviors` juga per-destinasi. Kalau behavior dipakai bersama, `contentOffset`
 * menumpuk lintas layar sehingga bar tetap frosted setelah pindah screen dan
 * baru reset kalau layar digulir dulu.
 */
class TopBarState {
    /** Teks kecil di atas judul (kategori layar), per route. */
    val eyebrow = mutableStateMapOf<String, String>()

    /** Judul layar, per route. */
    val title = mutableStateMapOf<String, String>()

    /** Scroll behavior milik destinasi, per route. */
    val behaviors = mutableStateMapOf<String, TopAppBarScrollBehavior>()
}

/**
 * [TopBarState] milik app. Disediakan [AppNavHost] di atas NavHost, jadi layar
 * bisa menulis identitasnya lewat [TopBarInfoEffect]; di luar [AppNavHost]
 * nilainya masih contoh default (kosong) dan top bar merender bar kosong.
 */
val LocalTopBarState = compositionLocalOf { TopBarState() }

/** Route destinasi yang sedang merender — kunci penulisan state top bar. */
val LocalScreenRoute = compositionLocalOf { "" }

/**
 * Lingkup per-destinasi. Di-remember DI DALAM back stack entry NavHost, jadi
 * `contentOffset` ikut di-restore bersama posisi scroll layar saat tab berpindah
 * (behavior default memakai rememberSaveable).
 */
@Composable
private fun ScreenScope(route: String, content: @Composable () -> Unit) {
    val state = LocalTopBarState.current
    val behavior = TopAppBarDefaults.pinnedScrollBehavior()
    // Daftarkan supaya top bar bisa membaca offset destinasi ini. Menulis
    // referensi yang sama tidak memicu invalidasi state map.
    SideEffect { state.behaviors[route] = behavior }
    CompositionLocalProvider(
        LocalTopAppBarScrollBehavior provides behavior,
        LocalScreenRoute provides route,
    ) { content() }
}

/**
 * Pindahkan header layar ke top bar.
 * Ganti pemakaian `ScreenHeader(title, subtitle)` dengan ini:
 * `TopBarInfoEffect(eyebrow = subtitle, title = title)`.
 */
@Composable
fun TopBarInfoEffect(eyebrow: String, title: String) {
    val state = LocalTopBarState.current
    val route = LocalScreenRoute.current
    SideEffect {
        state.eyebrow[route] = eyebrow
        state.title[route] = title
    }
}

// Komponen navigasi utama. NavController dimiliki di dalam body
// (bukan parameter) agar semua parameter composable tetap stabil.
// Injeksi ViewModel + load state dilakukan di call site tiap route,
// Screen hanya menerima data immutable dan callback.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppNavHost(
    modifier: Modifier = Modifier,
    startRoute: String = Routes.HOME,
) {
    val navController = rememberNavController()
    val back by navController.currentBackStackEntryAsState()
    val current: String? = back?.destination?.route

    // Back di tab root = konfirmasi keluar, bukan kembali ke tab sebelumnya.
    var confirmExit by remember { mutableStateOf(false) }
    val exit = rememberAppExit()
    // State top bar per-route; layar menulisnya via TopBarInfoEffect.
    val topBarState = remember { TopBarState() }

    // Zhitung kunjungan ke Beranda; naik lagi setiap kali tab diaktifkan kembali.
    var homeVisit by remember { mutableIntStateOf(0) }

    // Pindah tab: stack diratakan ke start destination dan state tiap tab
    // disimpan. Tanpa ini Home -> Stok menumpuk Stok di atas Home, sehingga
    // back balik ke Home. Dengan saveState/restoreState, keranjang Kasir dan
    // filter Stok tetap utuh saat pindah tab.
    val goToTab: (String) -> Unit = { route ->
        if (route != current) {
            navController.navigate(route) {
                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                launchSingleTop = true
                restoreState = true
            }
        }
    }

    // Navigasi non-tab (Riwayat, Pengeluaran, Detail Struk): tetap menumpuk di
    // atas tab asal supaya back-nya masuk akal.
    val goToScreen: (String) -> Unit = { route ->
        navController.navigate(route) { launchSingleTop = true }
    }

    // Dispatcher: route tab root (Kasir/Stok/Bon/More) lewat goToTab (switch +
    // saveState), sub-screen (Riwayat/Pengeluaran/Detail) lewat goToScreen (push).
    // Dashboard quick action + MoreScreen pakai ini supaya tab route ga ke-push
    // jadi sub-screen (yang bikin bottom bar Home no-op + back stack rusak).
    val goToRoute: (String) -> Unit = { route ->
        if (Routes.isTabRoute(route)) goToTab(route) else goToScreen(route)
    }

    // Tiap destinasi punya scroll behavior sendiri (lihat ScreenScope), jadi
    // tidak ada lagi behavior bersama yang perlu di-reset saat navigasi.

    Box(Modifier.fillMaxSize()) {
        Scaffold(
            // Backdrop blur: Modifier.blur memburam SELURUH subtree Scaffold, yaitu
            // konten app di belakang dialog. Dialog-nya sendiri adalah jendela
            // terpisah (android.ui.window.Dialog) sehingga TIDAK ikut ter-blur —
            // ini satu-satunya komposisi blur yang benar di Compose Multiplatform.
            //
            // Dinyalakan oleh `confirmExit` (dialog keluar) ATAU
            // GlassDialogBackdrop (dialog kaca mana pun, mis. konfirmasi hapus di
            // Stok). Tanpa yang kedua, latar dialog dari baris daftar tetap tajam
            // dan teksnya menembus panel — lihat catatan di Common.kt.
            //
            // Catatan minSdk 26: Modifier.blur no-op di bawah API 31. Di sana
            // kedua jalur sama-sama tidak ter-blur, jadi tidak ada ketimpangan;
            // yang hilang cuma kehalusan latarnya. Kalau itu suatu saat jadi
            // masalah, tambahkan scrim eksplisit di dalam dialog — bukan
            // menurunkan alpha panel.
            modifier = Modifier.blur(
                if (confirmExit || GlassDialogBackdrop.open) 20.dp else 0.dp,
            ),
            containerColor = Color.Transparent,
            // No automatic insets: content draws full-bleed behind the status bar,
            // top bar overlays it translucency-style (YouTube). Bottom bar is NOT
            // in the Scaffold slot — see below. IME handled per-screen on Android.
            contentWindowInsets = WindowInsets(0.dp),
            topBar = {},
            // bottomBar sengaja KOSONG. Kalau ditaruh di sini, Scaffold memesan
            // layout space setinggi bar sehingga konten berhenti di atasnya dan
            // tidak pernah lewat di BELAKANG kaca. Bar dirender sebagai overlay
            // sibling supaya konten bisa menggulir menembusnya.
            bottomBar = {},
        ) {
            // Overlay: full-bleed content behind the status bar + translucent top bar on top.
            // Only the bottom bar reserves layout space; top offset is handled per-screen
            // as scrollable content padding so lists slide underneath it.
            Box(Modifier.fillMaxSize()) {
                val overlayTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + TOP_BAR_CONTENT_HEIGHT
                // Footprint tab bar yang mengambang: margin bawah 12dp + tinggi bar 64dp,
                // di atas system nav bar.
                val navInset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
                val bottomBarFootprint = navInset + 12.dp + 64.dp
                // Jeda 24dp di atas tepi atas panel — bukan 8dp. Ini jarak antara
                // KONTEN dan tab bar: cukup lega supaya item terakhir tiap list
                // tidak menempel ke panel, dan tidak tertutup kalau panel
                // menebar bayangan.
                val overlayBottom = bottomBarFootprint + 24.dp
                // Behavior scroll diberikan per-destinasi lewat ScreenScope, bukan di sini.
                CompositionLocalProvider(
                    LocalTopOverlayPadding provides overlayTop,
                    LocalBottomOverlayPadding provides overlayBottom,
                    LocalTopBarState provides topBarState,
                ) {
                    NavHost(
                        navController = navController,
                        startDestination = startRoute,
                        modifier = modifier.fillMaxSize(),
                    ) {
                        composable(Routes.HOME) {
                            ScreenScope(Routes.HOME) {
                                val vm: DashboardViewModel = koinInject()
                                // Naik tiap kali Beranda menjadi tab aktif, jadi omzet/piutang
                                // selalu segar setelah cashier atau bon berubah di tab lain.
                                LaunchedEffect(Unit) { homeVisit++ }
                                val summary by vm.summary.collectAsState()
                                val top by vm.top.collectAsState()
                                val piutang by vm.piutang.collectAsState()
                                val products by vm.products.collectAsState()
                                val debts by vm.activeDebts.collectAsState()
                                val error by vm.error.collectAsState()

                                DashboardScreen(
                                    summary = summary,
                                    top = top.toPersistentList(),
                                    piutang = piutang,
                                    lowStock = products.filter { it.stock <= it.minStock || it.stock <= StockRules.LOW_STOCK_FALLBACK }
                                        .toPersistentList(),
                                    activeDebtCount = debts.size,
                                    error = error?.text().orEmpty(),
                                    onRefresh = vm::refresh,
                                    onQuickAction = goToRoute,
                                    refreshKey = homeVisit,
                                )
                            }
                        }
                        composable(Routes.CASHIER) {
                            ScreenScope(Routes.CASHIER) {
                                val vm: CashierViewModel = koinInject()
                                val cart by vm.cart.collectAsState()
                                val query by vm.query.collectAsState()
                                val category by vm.category.collectAsState()
                                val categories by vm.categories.collectAsState()
                                val visible by vm.visibleProducts.collectAsState()
                                val message by vm.message.collectAsState()
                                var showPay by remember { mutableStateOf(false) }

                                CashierScreen(
                                    query = query,
                                    onQuery = vm::search,
                                    categories = categories.toImmutableList(),
                                    activeCategory = category,
                                    onCategory = vm::selectCategory,
                                    list = visible.toImmutableList(),
                                    cart = cart.toImmutableMap(),
                                    lines = vm.cartLines().toImmutableList(),
                                    total = vm.cartTotal(),
                                    message = message?.text().orEmpty(),
                                    showPayDialog = showPay,
                                    onAdd = vm::add,
                                    onSetQty = vm::setQty,
                                    onClear = vm::clearCart,
                                    onPay = { showPay = true },
                                    onDismissPay = { showPay = false },
                                    onConfirmPay = { paid, method, customer ->
                                        vm.checkout(paid, method, customer)
                                        showPay = false
                                    },
                                )
                            }
                        }
                        composable(Routes.STOCK) {
                            ScreenScope(Routes.STOCK) {
                                val vm: ProductViewModel = koinInject()
                                val visible by vm.visibleProducts.collectAsState()
                                val query by vm.query.collectAsState()
                                val category by vm.category.collectAsState()
                                val categories by vm.categories.collectAsState()
                                val message by vm.message.collectAsState()
                                var showForm by remember { mutableStateOf(false) }
                                var edit: ProductEntity? by remember { mutableStateOf(null) }

                                ProductScreen(
                                    products = visible,
                                    query = query,
                                    onQuery = vm::search,
                                    categories = categories,
                                    activeCategory = category,
                                    onCategory = vm::selectCategory,
                                    message = message?.text().orEmpty(),
                                    showForm = showForm,
                                    edit = edit,
                                    onAdd = { edit = null; showForm = true },
                                    onEdit = { edit = it; showForm = true },
                                    onDelete = vm::delete,
                                    onDismissForm = { showForm = false },
                                    onSaveForm = { name, buy, sell, stock, unit, barcode, cat ->
                                        vm.save(name, buy, sell, stock, unit, barcode, cat, edit?.id ?: 0)
                                        showForm = false
                                    },
                                )
                            }
                        }
                        composable(Routes.HISTORY) {
                            ScreenScope(Routes.HISTORY) {
                                val vm: HistoryViewModel = koinInject()
                                val sales by vm.sales.collectAsState()
                                val category by vm.category.collectAsState()
                                val categories by vm.categories.collectAsState()
                                val loadingMore by vm.loadingMore.collectAsState()
                                val hasMore by vm.hasMore.collectAsState()
                                val rangeFrom by vm.rangeFrom.collectAsState()
                                val rangeTo by vm.rangeTo.collectAsState()
                                val exporting by vm.exporting.collectAsState()
                                val exportMessage by vm.exportMessage.collectAsState()

                                HistoryScreen(
                                    sales = sales,
                                    categories = categories,
                                    activeCategory = category,
                                    loadingMore = loadingMore,
                                    hasMore = hasMore,
                                    rangeFrom = rangeFrom,
                                    rangeTo = rangeTo,
                                    exporting = exporting,
                                    onCategory = vm::selectCategory,
                                    onRange = vm::selectRange,
                                    onExport = vm::exportPdf,
                                    onOpen = {
                                        vm.selectSale(it)
                                        goToScreen(Routes.HISTORY_DETAIL)
                                    },
                                    onLoadMore = vm::loadMore,
                                )

                                // Pesan hasil ekspor ditampilkan sekali lalu dibersihkan,
                                // supaya tidak menempel di layar setelah user pindah.
                                exportMessage?.let {
                                    LaunchedEffect(it) {
                                        delay(MESSAGE_VISIBLE_MILLIS)
                                        vm.clearExportMessage()
                                    }
                                }
                            }
                        }
                        composable(Routes.HISTORY_DETAIL) {
                            ScreenScope(Routes.HISTORY_DETAIL) {
                                val vm: HistoryViewModel = koinInject()
                                val selectedId by vm.selectedId.collectAsState()
                                val detail by vm.detail.collectAsState()
                                val message by vm.detailMessage.collectAsState()
                                var showEdit by remember { mutableStateOf(false) }

                                LaunchedEffect(selectedId) { vm.loadDetail(selectedId) }

                                SaleDetailScreen(
                                    detail = detail,
                                    message = message?.text().orEmpty(),
                                    showEditDialog = showEdit,
                                    onBack = { navController.popBackStack() },
                                    onEdit = { showEdit = true },
                                    onDismissEdit = { showEdit = false },
                                    onSaveEdit = { customer, note ->
                                        vm.updateInfo(selectedId, customer, note)
                                        showEdit = false
                                    },
                                )
                            }
                        }
                        composable(Routes.DEBT) {
                            ScreenScope(Routes.DEBT) {
                                val vm: DebtViewModel = koinInject()
                                val debts by vm.debts.collectAsState()
                                val message by vm.message.collectAsState()
                                var showAdd by remember { mutableStateOf(false) }
                                var payId by remember { mutableStateOf<Long?>(null) }

                                DebtScreen(
                                    debts = debts,
                                    message = message?.text().orEmpty(),
                                    showAddDialog = showAdd,
                                    payId = payId,
                                    onAdd = { showAdd = true },
                                    onPay = { payId = it },
                                    onDismissAdd = { showAdd = false },
                                    onSaveDebt = { name, amount, note ->
                                        vm.saveManual(name, amount, note)
                                        showAdd = false
                                    },
                                    onDismissPay = { payId = null },
                                    onSavePay = { amount ->
                                        payId?.let { vm.pay(it, amount) }
                                        payId = null
                                    },
                                )
                            }
                        }
                        composable(Routes.EXPENSE) {
                            ScreenScope(Routes.EXPENSE) {
                                val vm: ExpenseViewModel = koinInject()
                                val items by vm.items.collectAsState()
                                val message by vm.message.collectAsState()
                                val loadingMore by vm.loadingMore.collectAsState()
                                val hasMore by vm.hasMore.collectAsState()
                                var showAdd by remember { mutableStateOf(false) }

                                ExpenseScreen(
                                    items = items,
                                    message = message?.text().orEmpty(),
                                    showAddDialog = showAdd,
                                    loadingMore = loadingMore,
                                    hasMore = hasMore,
                                    onAdd = { showAdd = true },
                                    onDelete = vm::delete,
                                    onDismissAdd = { showAdd = false },
                                    onSaveExpense = { name, category, amount, note ->
                                        vm.save(name, category, amount, note)
                                        showAdd = false
                                    },
                                    onLoadMore = vm::loadMore,
                                )
                            }
                        }
                        composable(Routes.MORE) {
                            ScreenScope(Routes.MORE) {
                                MoreScreen(onNavigate = goToRoute)
                            }
                        }
                            }
                            // Top bar overlay (dibaca setelah NavHost = di atas konten).
                            // Semua nilai diambil dari bucket route AKTIF: judul/eyebrow
                            // ditulis layar lewat TopBarInfoEffect, frost dari scroll
                            // behavior milik destinasi itu sendiri — jadi tidak ada
                            // state yang bocor antar layar.
                            // Efek frosted top bar HANYA di Beranda.
                            // Di layar lain isi scrollable-nya LazyColumn ber-weight
                            // yang batas atasnya sudah di bawah bar, jadi tidak ada
                            // konten yang lewat di belakang bar — frost di sana
                            // tidak berguna. Beranda berbeda: scrollable-nya
                            // Column(verticalScroll) sepanjang layar, jadi kontennya
                            // memang lewat di belakang bar dan butuh frost.
                            val frostOffset = if (current == Routes.HOME) {
                                topBarState.behaviors[current]?.state?.contentOffset ?: 0f
                            } else {
                                0f
                            }
                            PremiumTopBar(
                                eyebrow = topBarState.eyebrow[current].orEmpty(),
                                title = topBarState.title[current].orEmpty(),
                                scrollOffset = frostOffset,
                                showBack = Routes.hasBackButton(current),
                                onBack = { navController.popBackStack() },
                            )
                        }
            }
        }
        // Tab bar sebagai overlay SIBLING — dirender setelah NavHost, di atasnya.
        // Karena Scaffold.bottomBar dikosongkan, konten sekarang bisa menggulir
        // menembus bar. Bar tetap mengambang di bagian bawah layar.
        LiquidGlassTabBar(
            current = current,
            onSelect = goToTab,
            modifier = Modifier.align(Alignment.BottomCenter),
        )
    }  // Box(Modifier.fillMaxSize())
    ExitOnBackFromTab(
        currentRoute = current,
        visible = confirmExit,
        onShow = { confirmExit = true },
        onDismiss = { confirmExit = false },
        onConfirm = {
            confirmExit = false
            exit()
        },
    )
}

/** Lama pesan sekali-jalan (hasil ekspor) tampil sebelum dibersihkan otomatis. */
private const val MESSAGE_VISIBLE_MILLIS = 2_500L

// ════════════════════════════════════════════════════════════════════════
//  LIQUID GLASS TAB BAR
// ════════════════════════════════════════════════════════════════════════
//  Ciri Liquid Glass yang diterapkan:
//   • Mengambang (floating island): margin horizontal + bawah, radius penuh.
//     Bar tidak lagi menempel edge-to-edge.
//   • Translucency: surface α 0.85 → kaca, tapi label tetap terbaca saat
//     konten lewat di belakangnya.
//   • Specular rim: border gradient putih-atas → gelap-bawah, bukan stroke
//     seragam — pantulan cahaya di tepi kaca.
//   • Shadow diffuse besar: bar terangkat dari konten.
//   • Pill aktif berlapis: gradient + rim dalam.
//
//  Catatan jujur soal blur: backdrop-blur ASLI (konten di belakang bar ikut
//  kabur) tidak dipakai. Alasannya sama dengan dialog dead-end #1 —
//  Modifier.blur di node bar justru memburam IKON dan LABEL bar sendiri.
//  Untuk floating bar, backdrop blur butuh snapshot konten di belakangnya,
//  terlalu rapuh cross-platform. Kaca di sini dibentuk dari translucency +
//  rim + shadow — sama bahasanya dengan kartu GlassCard (α 0.88) tapi lebih
//  tembus (α 0.55) karena bar menempel pada background polos, bukan pada
//  konten multi-warna yang tajam.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LiquidGlassTabBar(
    current: String?,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val scheme = MaterialTheme.colorScheme
    val isDark = scheme.surface.luminance() < 0.5f
    val shape = RoundedCornerShape(26.dp)

    // Ruang untuk gesture bar sistem supaya pulau tidak menempel paling bawah.
    val bottomInset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    // Wash konten di belakang panel kaca — area frost yang sama seperti
    // sebelumnya (selebar layar, dari tepi atas panel sampai bawah layar).
    // Yang diubah cuma bagaimana ia HABIS ke atas, bukan apakah ia ada.
    //
    // Kenapa ramp-nya panjang (44dp) dan alpha-nya "telat" di ujung atas:
    // background app ini WarungBackground ber-blob (3 blob di-blur 70dp), bukan
    // warna rata. Wash Paper datar bertemu blob di satu garis → di situlah
    // muncul "separation" yang kentara. Dengan ramp panjang + stop yang telat,
    // blob memudar perlahan ke wash dan tidak ada garis yang terbaca.
    // Efek sampingnya bagus: di 45% band alpha masih ~0.02, jadi konten yang
    // berhenti 24dp di atas panel (overlayBottom) TIDAK ikut kena wash.
    //
    // Warna: `background` (Paper #FAF6EE), BUKAN `surface` (CardWhite #FFFDF7).
    // Panel memakai `surface`; kalau wash juga `surface`, panel dan latarnya
    // jadi warna yang SAMA → beda luminance 1.009:1 dan pulau kacanya hilang
    // total. Paper lebih gelap, jadi panel terbaca lebih terang dari latarnya.
    // Terukur: beda panel-vs-latar 1.009:1 → 1.051:1; kontras label non-aktif
    // 4.35:1 (tanpa wash) → 5.87:1.
    val scrimFade = 44.dp
    val scrimBase = scheme.background.copy(alpha = if (isDark) 0.88f else 0.90f)
    Box(
        modifier
            .fillMaxWidth()
            .drawBehind {
                val fade = scrimFade.toPx()
                drawRect(
                    brush = Brush.verticalGradient(
                        colorStops = arrayOf(
                            0.00f to Color.Transparent,
                            0.40f to scrimBase.copy(alpha = scrimBase.alpha * 0.02f),
                            0.60f to scrimBase.copy(alpha = scrimBase.alpha * 0.10f),
                            0.80f to scrimBase.copy(alpha = scrimBase.alpha * 0.42f),
                            1.00f to scrimBase,
                        ),
                        startY = -fade,
                        endY = 0f,
                    ),
                    topLeft = Offset(0f, -fade),
                    size = Size(size.width, fade),
                )
                drawRect(
                    color = scrimBase,
                    topLeft = Offset(0f, 0f),
                    size = Size(size.width, size.height),
                )
            }
            .padding(
                start = 16.dp,
                end = 16.dp,
                bottom = bottomInset + 12.dp,
            ),
    ) {
        // Panel kaca memakai resep yang SAMA dengan ExitConfirmDialog dan
        // GlassCard: shadow → clip → tint → veil → specular sheen.
        // Border 1dp yang dulu ada di sini DIHAPUS — tepi kaca datang dari
        // sheen (specular 0.55 = "rim lit lip" seperti di dialog), bukan
        // dari stroke seragam.
        Box(
            Modifier
                .fillMaxWidth()
                .glassSurface(
                    shape = shape,
                    alpha = if (isDark) 0.80f else 0.85f,
                    elevation = 26.dp,
                    specular = 0.55f,
                )
                .padding(horizontal = 6.dp, vertical = 8.dp),
        ) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TABS.forEach { t ->
                    GlassTabItem(
                        tab = t,
                        selected = current == t.route,
                        isDark = isDark,
                        onClick = { onSelect(t.route) },
                    )
                }
            }
        }
    }
}

/**
 * Satu sel bottom nav, dirender berulang oleh `LiquidGlassTabBar` untuk setiap
 * entri [TABS]. State yang dibacanya cuma `selected` (dari route aktif) dan
 * `isDark`.
 *
 * `indication = null` disengaja: umpan balik tekan datang dari skala 0.94,
 * bukan ripple — ripple persegi akan keluar dari pill kacanya.
 */
@Composable
private fun GlassTabItem(
    tab: Tab,
    selected: Boolean,
    isDark: Boolean,
    onClick: () -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    val label = stringResource(tab.label)
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()

    Column(
        Modifier
            .clip(RoundedCornerShape(18.dp))
            .clickable(interaction, indication = null) { onClick() }
            .graphicsLayer {
                val s = if (pressed) 0.94f else 1f
                scaleX = s
                scaleY = s
            }
            .padding(horizontal = 10.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // Pill aktif memakai resep kaca yang sama, tanpa border putih: tepinya
        // dibentuk specular sheen di dalam pill (lihat glassSurface).
        Box(
            Modifier
                .size(width = 46.dp, height = 28.dp)
                .then(
                    if (selected) {
                        Modifier.glassSurface(
                            shape = CircleShape,
                            tint = scheme.primaryContainer,
                            alpha = 0.95f,
                            elevation = 8.dp,
                            specular = 0.45f,
                            veil = false,
                        )
                    } else {
                        Modifier
                    },
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                tab.icon.painter(),
                contentDescription = label,
                tint = if (selected) scheme.onPrimaryContainer else scheme.onSurfaceVariant,
                modifier = Modifier.size(22.dp),
            )
        }
        Spacer(Modifier.height(3.dp))
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            color = if (selected) scheme.primary else scheme.onSurfaceVariant,
            maxLines = 1,
        )
    }
}

/**
 * Top bar overlay: menampilkan IDENTITAS LAYAR (eyebrow + judul) yang ditulis
 * layar aktif lewat [TopBarInfoEffect]. Karena itu setiap layar tidak lagi
 * merender `ScreenHeader` sendiri — dulu judul tampil dua kali (sekali di top
 * bar, sekali di badan layar).
 *
 * Dua keadaan:
 *  • Di paling atas (scroll 0): TANPA latar → background app menembus sampai
 *    ke status bar.
 *  • Setelah di-scroll: FROSTED GLASS dengan bahasa visual yang sama seperti
 *    bottom tab bar — surface α 0.85 + rim specular + shadow diffuse. Tanpa ini
 *    konten menabrak jam status bar dan judulnya tidak terbaca.
 *
 * Sinyal frosted diambil dari `contentOffset`, BUKAN `collapsedFraction`:
 * PinnedScrollBehavior hanya memperbarui contentOffset dan tidak pernah
 * menyentuh heightOffset, sehingga collapsedFraction selalu 0 (lihat material3
 * AppBar.kt). contentOffset bertambah seiring scroll dan tidak butuh TopAppBar
 * material untuk ter-update.
 *
 * `scrollOffset` hanya bergerak kalau layar memanggil
 * Modifier.nestedScrollConnection(scrollBehavior, ...) pada container
 * scroll-nya.
 */
@Composable
private fun PremiumTopBar(
    eyebrow: String,
    title: String,
    scrollOffset: Float,
    showBack: Boolean = false,
    onBack: () -> Unit = {},
) {
    val scheme = MaterialTheme.colorScheme
    val surface = scheme.surface
    val isDark = surface.luminance() < 0.5f
    // Kaca penuh setelah ±120px scroll; di bawah itu bertahap.
    val glass = (kotlin.math.abs(scrollOffset) / 120f).coerceIn(0f, 1f)

    Box(
        Modifier
            .fillMaxWidth()
            .then(
                if (glass > 0.01f) {
                    Modifier.shadow(
                        elevation = 10.dp * glass,
                        shape = RectangleShape,
                        ambientColor = scheme.onSurface.copy(alpha = 0.10f),
                        spotColor = scheme.onSurface.copy(alpha = 0.16f),
                    )
                } else {
                    Modifier
                },
            ),
    ) {
        // Panel kaca: satu bidang menutup status bar + bar, sama seperti
        // perlakuan pada tab bar bawah.
        if (glass > 0.01f) {
            Box(
                Modifier
                    .matchParentSize()
                    .background(surface.copy(alpha = if (isDark) 0.80f else 0.85f * glass)),
            )
            // Rim specular di tepi bawah — pantulan cahaya, bukan stroke seragam.
            Box(
                Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                Color.White.copy(alpha = 0.55f * glass),
                                scheme.onSurface.copy(alpha = 0.10f * glass),
                            ),
                        ),
                    ),
            )
        }
        // Baris: [tombol back?] [eyebrow + judul]. Padding kiri dikurangi saat
        // tombolnya ada, karena IconButton sudah membawa padding sentuh 12dp-nya
        // sendiri — kalau tidak, judulnya terdorong terlalu jauh ke kanan
        // dibanding layar yang tanpa tombol back.
        Row(
            Modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.statusBars)
                .padding(
                    start = if (showBack) 8.dp else 20.dp,
                    end = 20.dp,
                    top = 10.dp,
                    bottom = 14.dp,
                ),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (showBack) {
                IconButton(onClick = onBack) {
                    Icon(
                        AppIcon.ARROW_BACK_IOS_NEW.painter(),
                        contentDescription = stringResource(Res.string.cd_back),
                        tint = scheme.onSurface,
                        modifier = Modifier.size(22.dp),
                    )
                }
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                if (eyebrow.isNotBlank()) {
                    Text(
                        eyebrow.uppercase(),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.2.sp,
                        color = scheme.primary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Text(
                    title,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-0.25).sp,
                    color = scheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

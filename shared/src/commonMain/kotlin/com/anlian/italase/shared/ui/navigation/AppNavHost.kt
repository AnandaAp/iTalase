package com.anlian.italase.shared.ui.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.font.FontWeight
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
import italase.shared.generated.resources.tab_cashier
import italase.shared.generated.resources.tab_debt
import italase.shared.generated.resources.tab_home
import italase.shared.generated.resources.tab_more
import italase.shared.generated.resources.tab_stock
import italase.shared.generated.resources.title_app
import italase.shared.generated.resources.title_cashier
import italase.shared.generated.resources.title_debts
import italase.shared.generated.resources.title_detail
import italase.shared.generated.resources.title_expenses
import italase.shared.generated.resources.title_history
import italase.shared.generated.resources.title_more
import italase.shared.generated.resources.title_products
import kotlinx.collections.immutable.toImmutableList
import kotlinx.collections.immutable.toImmutableMap
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject

private data class Tab(val route: String, val label: StringResource, val icon: AppIcon)

private val TABS = listOf(
    Tab(Routes.HOME, Res.string.tab_home, AppIcon.HOME),
    Tab(Routes.CASHIER, Res.string.tab_cashier, AppIcon.SHOPPING_CART),
    Tab(Routes.STOCK, Res.string.tab_stock, AppIcon.INVENTORY),
    Tab(Routes.DEBT, Res.string.tab_debt, AppIcon.HANDSHAKE),
    Tab(Routes.MORE, Res.string.tab_more, AppIcon.MORE),
)

@Composable
private fun titleFor(route: String?): String = when (route) {
    Routes.CASHIER -> stringResource(Res.string.title_cashier)
    Routes.STOCK -> stringResource(Res.string.title_products)
    Routes.HISTORY -> stringResource(Res.string.title_history)
    Routes.HISTORY_DETAIL -> stringResource(Res.string.title_detail)
    Routes.DEBT -> stringResource(Res.string.title_debts)
    Routes.EXPENSE -> stringResource(Res.string.title_expenses)
    Routes.MORE -> stringResource(Res.string.title_more)
    else -> stringResource(Res.string.title_app)
}

// CompositionLocal to provide top app bar scroll behavior to descendant screens
val LocalTopAppBarScrollBehavior = compositionLocalOf<TopAppBarScrollBehavior?> { null }

// Top offset (status bar height + overlay app bar height) so screen content
// starts below the translucent overlay bar but scrolls underneath it (YouTube-style).
val LocalTopOverlayPadding = compositionLocalOf { 64.dp }

// Bottom offset (tab bar height + its margin + system nav bar) so scrollable
// screens can add trailing padding and scroll their last row ABOVE the floating
// glass bar instead of leaving it permanently hidden underneath.
val LocalBottomOverlayPadding = compositionLocalOf { 0.dp }

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

    // YouTube-style: top app bar hides on scroll down, shows on scroll up
    // pinnedScrollBehavior, bukan enterAlways: bar tidak lagi menyembunyikan
    // diri saat scroll. Ia tetap di tempat dan bertransisi dari tembus
    // (di atas) menjadi frosted glass (saat konten lewat di belakangnya).
    // enterAlways membuat judul hilang lalu konten menabrak status bar —
    // persis masalah yang muncul di screenshot.
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()

    // Each destination starts with an expanded bar: the behavior is shared
    // across routes, so reset it on navigation. Otherwise a bar hidden on
    // Home would stay hidden on Kasir/Stok.
    LaunchedEffect(current) {
        scrollBehavior.state.heightOffset = 0f
        scrollBehavior.state.contentOffset = 0f
    }

    Box(Modifier.fillMaxSize()) {
    Scaffold(
        // Backdrop blur: Modifier.blur memburam SELURUH subtree Scaffold, yaitu
        // konten app di belakang dialog. Dialog-nya sendiri adalah jendela
        // terpisah (android.ui.window.Dialog) sehingga TIDAK ikut ter-blur —
        // ini satu-satunya komposisi blur yang benar di Compose Multiplatform.
        modifier = Modifier.blur(if (confirmExit) 20.dp else 0.dp),
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
            val overlayTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + 64.dp
            // Footprint tab bar yang mengambang: margin bawah 12dp + tinggi bar 64dp,
            // di atas system nav bar. Ditambah jeda 8dp jadi ruang gulir minimum.
            val navInset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
            val bottomBarFootprint = navInset + 12.dp + 64.dp
            val overlayBottom = bottomBarFootprint + 8.dp
            // Provide scroll behavior to all screens via CompositionLocal
            androidx.compose.runtime.CompositionLocalProvider(
                LocalTopAppBarScrollBehavior provides scrollBehavior,
                LocalTopOverlayPadding provides overlayTop,
                LocalBottomOverlayPadding provides overlayBottom,
            ) {
                NavHost(
                    navController = navController,
                    startDestination = startRoute,
                    modifier = modifier.fillMaxSize(),
                ) {
            composable(Routes.HOME) {
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
                    top = top,
                    piutang = piutang,
                    lowStock = products.filter { it.stock <= it.minStock || it.stock <= StockRules.LOW_STOCK_FALLBACK },
                    activeDebtCount = debts.size,
                    error = error?.text().orEmpty(),
                    onRefresh = vm::refresh,
                    onQuickAction = goToRoute,
                    refreshKey = homeVisit,
                )
            }
            composable(Routes.CASHIER) {
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
            composable(Routes.STOCK) {
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
            composable(Routes.HISTORY) {
                val vm: HistoryViewModel = koinInject()
                val sales by vm.sales.collectAsState()
                val category by vm.category.collectAsState()
                val categories by vm.categories.collectAsState()

                HistoryScreen(
                    sales = sales,
                    categories = categories,
                    activeCategory = category,
                    onCategory = vm::selectCategory,
                    onOpen = {
                        vm.selectSale(it)
                        goToScreen(Routes.HISTORY_DETAIL)
                    },
                )
            }
            composable(Routes.HISTORY_DETAIL) {
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
            composable(Routes.DEBT) {
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
            composable(Routes.EXPENSE) {
                val vm: ExpenseViewModel = koinInject()
                val items by vm.items.collectAsState()
                val message by vm.message.collectAsState()
                var showAdd by remember { mutableStateOf(false) }

                ExpenseScreen(
                    items = items,
                    message = message?.text().orEmpty(),
                    showAddDialog = showAdd,
                    onAdd = { showAdd = true },
                    onDelete = vm::delete,
                    onDismissAdd = { showAdd = false },
                    onSaveExpense = { name, category, amount, note ->
                        vm.save(name, category, amount, note)
                        showAdd = false
                    },
                )
            }
            composable(Routes.MORE) {
                MoreScreen(onNavigate = goToRoute)
            }
                }
                // Premium frosted-glass overlay top bar (drawn after NavHost = on top).
                // TopAppBar keeps its default statusBars window inset, so it sits
                // below the system icons.
                PremiumTopBar(title = titleFor(current), scrollBehavior = scrollBehavior)
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

    Box(
        modifier
            .fillMaxWidth()
            .padding(
                start = 16.dp,
                end = 16.dp,
                bottom = bottomInset + 12.dp,
            ),
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .shadow(
                    elevation = 26.dp,
                    shape = shape,
                    ambientColor = scheme.onSurface.copy(alpha = 0.14f),
                    spotColor = scheme.onSurface.copy(alpha = 0.22f),
                )
                .clip(shape)
                // Translucency: cukup tembus untuk terlihat kaca, cukup pekat
                // untuk menjaga label tetap terbaca saat konten lewat di
                // belakangnya. 0.55 terbukti terlalu bening (teks konten
                // bertabrakan dengan label tab), jadi dinaikkan.
                .background(scheme.surface.copy(alpha = if (isDark) 0.80f else 0.85f))
                .border(
                    width = 1.dp,
                    brush = Brush.verticalGradient(
                        listOf(
                            Color.White.copy(alpha = if (isDark) 0.30f else 0.62f),
                            scheme.onSurface.copy(alpha = 0.08f),
                        ),
                    ),
                    shape = shape,
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
        // Pill aktif berlapis: radial highlight + rim dalam + shadow bertint.
        Box(
            Modifier
                .size(width = 46.dp, height = 28.dp)
                .clip(CircleShape)
                .then(
                    if (selected) {
                        Modifier
                            .background(
                                Brush.verticalGradient(
                                    listOf(
                                        scheme.primaryContainer,
                                        scheme.primaryContainer.copy(alpha = 0.72f),
                                    ),
                                ),
                            )
                            .border(
                                width = 1.dp,
                                color = Color.White.copy(alpha = if (isDark) 0.18f else 0.55f),
                                shape = CircleShape,
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
 * Top bar overlay. Dua keadaan:
 *
 *  • Di paling atas (collapse 0): TANPA latar → background app menembus sampai
 *    ke status bar (sesuai catatan: top system bar harus tembus).
 *
 *  • Setelah di-scroll (collapse naik): menjadi FROSTED GLASS dengan bahasa
 *    visual yang sama seperti bottom tab bar — surface α 0.85 + rim specular +
 *    shadow diffuse. Tanpa ini konten yang menggulir menabrak jam status bar
 *    dan judul (jelek, dan judul jadi tidak terbaca).
 *
 * CATATAN PENTING: nilai `collapse` hanya bergerak kalau layar memanggil
 * Modifier.nestedScrollConnection(scrollBehavior, ...) pada container
 * scroll-nya. Layar yang tidak memasangnya akan selalu collapse = 0, sehingga
 * top bar tetap tembus dan konten menabrak status bar.
 */
@Composable
private fun PremiumTopBar(
    title: String,
    scrollBehavior: TopAppBarScrollBehavior?,
) {
    val scheme = MaterialTheme.colorScheme
    val surface = scheme.surface
    val isDark = surface.luminance() < 0.5f
    // PENTING — kenapa overlappedFraction, bukan collapsedFraction:
    // PinnedScrollBehavior hanya memperbarui contentOffset dan TIDAK pernah
    // menyentuh heightOffset (lihat material3 AppBar.kt, PinnedScrollBehavior).
    // collapsedFraction = heightOffset / heightOffsetLimit = 0 / -barHeight,
    // jadi SELALU 0 → frosted glass tidak pernah terpicu.
    // overlappedFraction justru didefinisikan sebagai "persentase area app bar
    // yang ter-overlap konten yang di-scroll di belakangnya": 0 saat di paling
    // atas, naik ke 1 saat konten mulai lewat. Persis sinyal yang dibutuhkan.
    val glass = (scrollBehavior?.state?.overlappedFraction ?: 0f).coerceIn(0f, 1f)

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
        CenterAlignedTopAppBar(
            title = {
                Text(
                    title,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 0.25.sp,
                    maxLines = 1,
                )
            },
            scrollBehavior = scrollBehavior,
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = Color.Transparent,
                scrolledContainerColor = Color.Transparent,
                navigationIconContentColor = Color.Unspecified,
                titleContentColor = scheme.onSurface,
                actionIconContentColor = Color.Unspecified,
            ),
        )
    }
}

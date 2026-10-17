package com.anlian.italase.shared.ui.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsTopHeight
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
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
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()

    // Each destination starts with an expanded bar: the behavior is shared
    // across routes, so reset it on navigation. Otherwise a bar hidden on
    // Home would stay hidden on Kasir/Stok.
    LaunchedEffect(current) {
        scrollBehavior.state.heightOffset = 0f
        scrollBehavior.state.contentOffset = 0f
    }

    Scaffold(
        // Backdrop blur: Modifier.blur memburam SELURUH subtree Scaffold, yaitu
        // konten app di belakang dialog. Dialog-nya sendiri adalah jendela
        // terpisah (android.ui.window.Dialog) sehingga TIDAK ikut ter-blur —
        // ini satu-satunya komposisi blur yang benar di Compose Multiplatform.
        modifier = Modifier.blur(if (confirmExit) 20.dp else 0.dp),
        containerColor = Color.Transparent,
        // No automatic insets: content draws full-bleed behind the status bar,
        // top bar overlays it translucency-style (YouTube). Bottom bar keeps
        // its Scaffold slot so lists end above it. IME handled per-screen on Android.
        contentWindowInsets = WindowInsets(0.dp),
        topBar = {},
        bottomBar = {
            NavigationBar(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f)) {
                TABS.forEach { t ->
                    val label = stringResource(t.label)
                    NavigationBarItem(
                        selected = current == t.route,
                        onClick = { goToTab(t.route) },
                        icon = { Icon(t.icon.painter(), contentDescription = label) },
                        label = { Text(label) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                        ),
                    )
                }
            }
        },
    ) { pad ->
        // Overlay: full-bleed content behind the status bar + translucent top bar on top.
        // Only the bottom bar reserves layout space; top offset is handled per-screen
        // as scrollable content padding so lists slide underneath it.
        Box(
            Modifier
                .fillMaxSize()
                .padding(bottom = pad.calculateBottomPadding()),
        ) {
            val overlayTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + 64.dp
            // Provide scroll behavior to all screens via CompositionLocal
            androidx.compose.runtime.CompositionLocalProvider(
                LocalTopAppBarScrollBehavior provides scrollBehavior,
                LocalTopOverlayPadding provides overlayTop,
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

// Frosted-glass top bar: the bar unit (scrim + content) slides away together on
// scroll, while a fixed system-bar scrim stays put so the status bar is always
// translucent — even when the bar itself is hidden. On scroll the scrim
// solidifies and a hairline divider fades in — the iOS / premium Android feel.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PremiumTopBar(
    title: String,
    scrollBehavior: TopAppBarScrollBehavior?,
) {
    val surface = MaterialTheme.colorScheme.surface
    // 0f expanded → 1f fully collapsed on scroll.
    val collapse = scrollBehavior?.state?.collapsedFraction ?: 0f
    // Same translation the TopAppBar applies internally, so scrim and bar move as one.
    val barOffset = scrollBehavior?.state?.heightOffset ?: 0f
    val topAlpha = 0.94f + 0.06f * collapse
    val bottomAlpha = 0.86f + 0.14f * collapse
    Box(Modifier.fillMaxWidth()) {
        // Fixed status scrim: never moves, keeps the system bar translucent
        // with content faintly melting underneath when the bar is hidden.
        Box(
            Modifier
                .fillMaxWidth()
                .windowInsetsTopHeight(TopAppBarDefaults.windowInsets)
                .background(
                    Brush.verticalGradient(
                        0f to surface.copy(alpha = 0.90f),
                        1f to surface.copy(alpha = 0.60f),
                    ),
                ),
        )
        // Moving bar unit.
        Box(
            Modifier
                .fillMaxWidth()
                .graphicsLayer { translationY = barOffset },
        ) {
            Box(
                Modifier.matchParentSize().background(
                    Brush.verticalGradient(
                        0f to surface.copy(alpha = topAlpha),
                        0.7f to surface.copy(alpha = (topAlpha + bottomAlpha) / 2f),
                        1f to surface.copy(alpha = bottomAlpha),
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
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                    actionIconContentColor = Color.Unspecified,
                ),
            )
            if (collapse > 0.02f) {
                HorizontalDivider(
                    modifier = Modifier.align(Alignment.BottomCenter),
                    thickness = Dp.Hairline,
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f * collapse),
                )
            }
        }
    }
}

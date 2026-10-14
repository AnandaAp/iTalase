package com.anlian.italase.shared.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.anlian.italase.shared.data.StockRules
import com.anlian.italase.shared.data.local.entity.ProductEntity
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
import italase.shared.generated.resources.title_expenses
import italase.shared.generated.resources.title_detail
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
    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(titleFor(current), fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.80f),
                    scrolledContainerColor = Color.Unspecified,
                    navigationIconContentColor = Color.Unspecified,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                    actionIconContentColor = Color.Unspecified
                ),
            )
        },
        bottomBar = {
            NavigationBar(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f)) {
                TABS.forEach { t ->
                    val label = stringResource(t.label)
                    NavigationBarItem(
                        selected = current == t.route,
                        onClick = { if (current != t.route) navController.navigate(t.route) },
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
        NavHost(
            navController = navController,
            startDestination = startRoute,
            modifier = modifier.padding(pad),
        ) {
            composable(Routes.HOME) {
                val vm: DashboardViewModel = koinInject()
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
                    onQuickAction = { navController.navigate(route = it) },
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
                        navController.navigate(route = Routes.HISTORY_DETAIL)
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
                MoreScreen(onNavigate = { navController.navigate(route = it) })
            }
        }
    }
}

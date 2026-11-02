package com.anlian.italase.shared.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.style.TextAlign
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.anlian.italase.shared.data.local.entity.QrisConfigEntity
import com.anlian.italase.shared.ui.components.LocalIsLandscape
import com.anlian.italase.shared.ui.state.QrisViewModel
import org.jetbrains.compose.resources.decodeToImageBitmap
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.anlian.italase.shared.data.local.entity.PayMethod
import com.anlian.italase.shared.data.local.entity.ProductEntity
import com.anlian.italase.shared.data.repository.CartLine
import com.anlian.italase.shared.ui.components.AppIcon
import com.anlian.italase.shared.ui.components.CategoryChips
import com.anlian.italase.shared.ui.components.GLASS_RADIUS
import com.anlian.italase.shared.ui.components.GlassButton
import italase.shared.generated.resources.tab_cashier
import italase.shared.generated.resources.detail_total
import com.anlian.italase.shared.ui.navigation.LocalBottomOverlayPadding
import com.anlian.italase.shared.ui.components.GlassCard
import androidx.compose.material3.ColorScheme
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.fillMaxHeight
import com.anlian.italase.shared.ui.components.StatusChip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.BoxWithConstraints
import com.anlian.italase.shared.ui.components.GlassChip
import com.anlian.italase.shared.ui.components.GlassDialog
import com.anlian.italase.shared.ui.components.GlassFormDialog
import com.anlian.italase.shared.ui.components.GlassEmptyState
import com.anlian.italase.shared.ui.components.GlassShimmerRow
import com.anlian.italase.shared.ui.components.ShimmerCrossfade
import com.anlian.italase.shared.ui.components.GlassIconButton
import com.anlian.italase.shared.ui.components.GlassRow
import com.anlian.italase.shared.ui.components.GlassTextField
import com.anlian.italase.shared.ui.components.enter
import com.anlian.italase.shared.ui.components.glassSurface
import com.anlian.italase.shared.ui.components.painter
import com.anlian.italase.shared.ui.components.rememberAppear
import com.anlian.italase.shared.ui.navigation.LocalTopAppBarScrollBehavior
import com.anlian.italase.shared.ui.navigation.LocalTopOverlayPadding
import com.anlian.italase.shared.ui.navigation.TopBarInfoEffect
import com.anlian.italase.shared.ui.navigation.aboveTabBar
import com.anlian.italase.shared.ui.navigation.bottomActionTailPadding
import com.anlian.italase.shared.ui.navigation.nestedScrollConnection
import com.anlian.italase.shared.util.formatRupiah
import com.anlian.italase.shared.util.parseRupiah
import italase.shared.generated.resources.Res
import italase.shared.generated.resources.action_cancel
import italase.shared.generated.resources.action_close
import italase.shared.generated.resources.action_pay
import italase.shared.generated.resources.action_save
import italase.shared.generated.resources.cash_add_cart
import italase.shared.generated.resources.cash_cart_items
import italase.shared.generated.resources.cash_change
import italase.shared.generated.resources.cash_customer
import italase.shared.generated.resources.cash_empty_h
import italase.shared.generated.resources.cash_empty_t
import italase.shared.generated.resources.cash_m_cash
import italase.shared.generated.resources.cash_m_transfer
import italase.shared.generated.resources.cash_paid
import italase.shared.generated.resources.cash_pay_title
import italase.shared.generated.resources.cash_product_line
import italase.shared.generated.resources.cash_search
import italase.shared.generated.resources.cash_short
import italase.shared.generated.resources.cash_short_hint
import italase.shared.generated.resources.cash_sub
import italase.shared.generated.resources.cd_add
import italase.shared.generated.resources.cd_remove
import italase.shared.generated.resources.cd_search
import italase.shared.generated.resources.prod_count
import italase.shared.generated.resources.prod_out
import italase.shared.generated.resources.tab_debt
import italase.shared.generated.resources.title_cashier
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.ImmutableMap
import org.jetbrains.compose.resources.stringResource

// Tinggi perkiraan kartu checkout. Dipakai untuk ruang ekor list (lihat
// bottomActionTailPadding), bukan untuk tinggi layout kartunya sendiri.
private val CHECKOUT_CARD_HEIGHT = 148.dp

/**
 * Banner prompt inisiasi QRIS di layar Kasir (AUD-107).
 * Mengikuti prinsip desain Warung Liquid Glass.
 */
@Composable
fun QrisPromptBanner(
    onNavigateSetup: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scheme = MaterialTheme.colorScheme
    val isDark = scheme.surface.luminance() < 0.5f

    Row(
        modifier = modifier
            .fillMaxWidth()
            .glassSurface(
                shape = RoundedCornerShape(20.dp),
                tint = scheme.surface,
                alpha = if (isDark) 0.88f else 0.94f,
                elevation = 8.dp,
                specular = 0.32f,
                veil = true,
            )
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Badge Ikon Emerald
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            scheme.primary.copy(alpha = if (isDark) 0.28f else 0.18f),
                            scheme.primary.copy(alpha = if (isDark) 0.12f else 0.08f),
                        )
                    )
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                AppIcon.PAYMENTS.painter(),
                contentDescription = null,
                tint = scheme.primary,
                modifier = Modifier.size(22.dp),
            )
        }

        Spacer(Modifier.width(12.dp))

        // Label Edukatif
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = "Aktifkan QRIS",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = scheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = "Terima pembayaran non-tunai di kasir",
                style = MaterialTheme.typography.bodySmall,
                color = scheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }

        Spacer(Modifier.width(8.dp))

        // Tombol Aksi
        GlassButton(
            text = "Aktifkan",
            onClick = onNavigateSetup,
            height = 36.dp,
            elevation = 4.dp,
            specular = 0.26f,
            modifier = Modifier.padding(end = 4.dp),
        )

        // Tombol Dismiss '✕'
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onDismiss,
                ),
            contentAlignment = Alignment.Center,
        ) {
            Canvas(modifier = Modifier.size(12.dp)) {
                val strokeWidth = 1.8.dp.toPx()
                val color = scheme.onSurfaceVariant.copy(alpha = 0.75f)
                drawLine(color, Offset(0f, 0f), Offset(size.width, size.height), strokeWidth = strokeWidth)
                drawLine(color, Offset(0f, size.height), Offset(size.width, 0f), strokeWidth = strokeWidth)
            }
        }
    }
}

/**
 * Entry point layar Kasir: daftar produk + keranjang, dan dialog bayar saat
 * [showPayDialog] true. Dipasang NavHost setelah state-nya dirakit ViewModel.
 *
 * Layar ini sengaja tak punya state sendiri — query, kategori aktif, keranjang,
 * dan total dipegang pemanggil supaya [CashierContent] bisa dipratinjau. Karena
 * itu semua callback diberi default kosong: pratinjau cukup mengisi state.
 *
 * @param onConfirmPay dipanggil dengan (nominal dibayar, metode, nama pelanggan).
 */
@Composable
fun CashierScreen(
    query: String,
    onQuery: (String) -> Unit,
    categories: ImmutableList<String>,
    activeCategory: String?,
    onCategory: (String?) -> Unit,
    list: ImmutableList<ProductEntity>,
    cart: ImmutableMap<Long, Int>,
    lines: ImmutableList<CartLine>,
    total: Long,
    message: String,
    showPayDialog: Boolean,
    isLoading: Boolean = false,
    qrisConfig: QrisConfigEntity? = null,
    qrisVm: QrisViewModel? = null,
    showQrisPrompt: Boolean? = null,
    onNavigateQrisSetup: () -> Unit = {},
    onDismissQrisPrompt: () -> Unit = {},
    onAdd: (ProductEntity) -> Unit = {},
    onSetQty: (Long, Int) -> Unit = { _, _ -> },
    onClear: () -> Unit = {},
    onPay: () -> Unit = {},
    onDismissPay: () -> Unit = {},
    onConfirmPay: (Long, String, String) -> Unit = { _, _, _ -> },
) {
    val vmConfig = qrisVm?.qrisConfig?.collectAsStateWithLifecycle()?.value
    val effectiveConfig = qrisConfig ?: vmConfig

    var dismissedThisSession by rememberSaveable { mutableStateOf(false) }

    val isQrisActive = effectiveConfig != null && effectiveConfig.isEnabled && effectiveConfig.imageBytes != null
    val isPromptPermanentlyDismissed = (effectiveConfig?.promptDismissCount ?: 0) >= 3
    val bannerVisible = showQrisPrompt ?: (!isQrisActive && !isPromptPermanentlyDismissed && !dismissedThisSession)

    CashierContent(
        query = query,
        onQuery = onQuery,
        categories = categories,
        activeCategory = activeCategory,
        onCategory = onCategory,
        list = list,
        cart = cart,
        lines = lines,
        total = total,
        message = message,
        isLoading = isLoading,
        showQrisPrompt = bannerVisible,
        onNavigateQrisSetup = onNavigateQrisSetup,
        onDismissQrisPrompt = {
            dismissedThisSession = true
            qrisVm?.dismissPrompt()
            onDismissQrisPrompt()
        },
        onAdd = onAdd,
        onSetQty = onSetQty,
        onClear = onClear,
        onPay = onPay,
    )

    if (showPayDialog) {
        PayDialog(
            total = total,
            qrisConfig = effectiveConfig,
            onDismiss = onDismissPay,
            onConfirm = onConfirmPay,
        )
    }
}

/**
 * Isi layar Kasir tanpa dialog bayar — versi yang aman dipratinjau. Dipanggil
 * [CashierScreen]; NavHost tidak memanggilnya langsung.
 *
 * Header hidup di top bar lewat [TopBarInfoEffect] (tidak merender apa pun di
 * sini). Daftar diberi ruang ekor setinggi kartu checkout karena kartu itu
 * mengambang di atas konten, dan tab bar ada di lapisan lebih atas lagi.
 *
 * @param list hasil pencarian + filter dari pemanggil; layar ini tidak
 *   menyaring ulang, hanya menggambar.
 */
@Composable
fun CashierContent(
    query: String,
    onQuery: (String) -> Unit,
    categories: List<String>,
    activeCategory: String?,
    onCategory: (String?) -> Unit,
    list: List<ProductEntity>,
    cart: Map<Long, Int>,
    lines: List<CartLine>,
    total: Long,
    message: String,
    isLoading: Boolean = false,
    showQrisPrompt: Boolean = false,
    onNavigateQrisSetup: () -> Unit = {},
    onDismissQrisPrompt: () -> Unit = {},
    onAdd: (ProductEntity) -> Unit = {},
    onSetQty: (Long, Int) -> Unit = { _, _ -> },
    onClear: () -> Unit = {},
    onPay: () -> Unit = {},
) {
    val scheme = MaterialTheme.colorScheme
    val scrollBehavior = LocalTopAppBarScrollBehavior.current
    val listState = rememberLazyListState()
    // Layar ini punya kartu checkout yang mengambang DI ATAS tab bar. Jadi list
    // butuh dua lapis ruang gulir: tinggi tab bar (agar item bisa lewat di
    // belakang tab bar) + tinggi kartu checkout (agar item terakhir tidak
    // tertutup permanen oleh keduanya).
    // Jarak & tinggi tab bar datang dari helper yang SAMA dengan layar ber-FAB,
    // supaya jarak aksi-vs-tab-bar identik di seluruh app.
    val checkoutSpace = bottomActionTailPadding(CHECKOUT_CARD_HEIGHT)

    // Header layar hidup di top bar.
    val appeared = rememberAppear(!isLoading)
    val cartCount = lines.sumOf { it.qty }
    val cashierEyebrow = if (cartCount > 0) {
        stringResource(Res.string.cash_cart_items, cartCount)
    } else {
        stringResource(Res.string.prod_count, list.size)
    }
    TopBarInfoEffect(
        eyebrow = cashierEyebrow,
        title = stringResource(Res.string.title_cashier),
    )

    BoxWithConstraints(Modifier.fillMaxSize()) {
        val isLandscape = (maxWidth > maxHeight && maxWidth >= 540.dp) || maxWidth >= 600.dp

        if (isLandscape) {
            // Tata letak Landscape POS: Split screen Master-Detail
            // Kiri: Katalog & Pencarian, Kanan: Keranjang & Checkout
            Row(
                Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                // Kolom Kiri: Katalog Produk
                Column(
                    Modifier
                        .weight(1.15f)
                        .fillMaxHeight()
                        .nestedScrollConnection(scrollBehavior, listState),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Spacer(Modifier.height(LocalTopOverlayPadding.current + 4.dp))
                    GlassTextField(
                        value = query,
                        onValueChange = onQuery,
                        label = stringResource(Res.string.cash_search),
                        leadingIcon = {
                            Icon(
                                AppIcon.SEARCH.painter(),
                                contentDescription = stringResource(Res.string.cd_search),
                            )
                        },
                        modifier = Modifier.enter(appeared, 0),
                    )
                    CategoryChips(
                        categories = categories,
                        active = activeCategory,
                        onSelect = onCategory,
                    )
                    AnimatedVisibility(
                        visible = showQrisPrompt,
                        enter = expandVertically(spring(stiffness = Spring.StiffnessMediumLow, dampingRatio = Spring.DampingRatioNoBouncy)) + fadeIn(),
                        exit = shrinkVertically(spring(stiffness = Spring.StiffnessMediumLow, dampingRatio = Spring.DampingRatioNoBouncy)) + fadeOut(),
                    ) {
                        QrisPromptBanner(
                            onNavigateSetup = onNavigateQrisSetup,
                            onDismiss = onDismissQrisPrompt,
                            modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp),
                        )
                    }
                    ShimmerCrossfade(
                        loading = isLoading,
                        modifier = Modifier.weight(1f),
                        shimmer = {
                            Column(
                                modifier = Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                repeat(6) { GlassShimmerRow() }
                            }
                        },
                        content = {
                            if (list.isEmpty()) {
                                GlassEmptyState(
                                    stringResource(Res.string.cash_empty_t),
                                    stringResource(Res.string.cash_empty_h),
                                )
                            } else {
                                LazyColumn(
                                    state = listState,
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .nestedScrollConnection(scrollBehavior, listState),
                                    verticalArrangement = Arrangement.spacedBy(8.dp),
                                    contentPadding = PaddingValues(bottom = LocalBottomOverlayPadding.current + 12.dp),
                                ) {
                                    items(list, key = { it.id }) { p ->
                                        val qty = cart[p.id] ?: 0
                                        ProductCashierRow(
                                            product = p,
                                            qty = qty,
                                            scheme = scheme,
                                            onAdd = onAdd,
                                            onSetQty = onSetQty,
                                        )
                                    }
                                }
                            }
                        }
                    )
                }

                // Kolom Kanan: Panel Keranjang POS
                Column(
                    Modifier
                        .weight(0.85f)
                        .fillMaxHeight(),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Spacer(Modifier.height(LocalTopOverlayPadding.current + 4.dp))
                    LandscapeCartPanel(
                        lines = lines,
                        total = total,
                        message = message,
                        cartCount = cartCount,
                        onSetQty = onSetQty,
                        onClear = onClear,
                        onPay = onPay,
                        modifier = Modifier
                            .weight(1f)
                            .enter(appeared, 2),
                    )
                    Spacer(Modifier.height(LocalBottomOverlayPadding.current))
                }
            }
        } else {
            // Tata letak Portrait: Original full width list + Floating CheckoutCard
            Box(Modifier.fillMaxSize()) {
                Column(
                    Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp)
                        .nestedScrollConnection(scrollBehavior, listState),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Spacer(Modifier.height(LocalTopOverlayPadding.current + 8.dp))
                    GlassTextField(
                        value = query,
                        onValueChange = onQuery,
                        label = stringResource(Res.string.cash_search),
                        leadingIcon = {
                            Icon(
                                AppIcon.SEARCH.painter(),
                                contentDescription = stringResource(Res.string.cd_search),
                            )
                        },
                        modifier = Modifier.enter(appeared, 0),
                    )
                    CategoryChips(
                        categories = categories,
                        active = activeCategory,
                        onSelect = onCategory,
                    )
                    AnimatedVisibility(
                        visible = showQrisPrompt,
                        enter = expandVertically(spring(stiffness = Spring.StiffnessMediumLow, dampingRatio = Spring.DampingRatioNoBouncy)) + fadeIn(),
                        exit = shrinkVertically(spring(stiffness = Spring.StiffnessMediumLow, dampingRatio = Spring.DampingRatioNoBouncy)) + fadeOut(),
                    ) {
                        QrisPromptBanner(
                            onNavigateSetup = onNavigateQrisSetup,
                            onDismiss = onDismissQrisPrompt,
                            modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp),
                        )
                    }
                    ShimmerCrossfade(
                        loading = isLoading,
                        modifier = Modifier.weight(1f),
                        shimmer = {
                            Column(
                                modifier = Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                repeat(6) { GlassShimmerRow() }
                            }
                        },
                        content = {
                            if (list.isEmpty()) {
                                GlassEmptyState(
                                    stringResource(Res.string.cash_empty_t),
                                    stringResource(Res.string.cash_empty_h),
                                )
                            } else {
                                LazyColumn(
                                    state = listState,
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .nestedScrollConnection(scrollBehavior, listState),
                                    verticalArrangement = Arrangement.spacedBy(8.dp),
                                    contentPadding = checkoutSpace,
                                ) {
                                    items(list, key = { it.id }) { p ->
                                        val qty = cart[p.id] ?: 0
                                        ProductCashierRow(
                                            product = p,
                                            qty = qty,
                                            scheme = scheme,
                                            onAdd = onAdd,
                                            onSetQty = onSetQty,
                                        )
                                    }
                                }
                            }
                        }
                    )
                }

                // Checkout card mengambang di atas konten
                CheckoutCard(
                    lines = lines.size,
                    total = total,
                    message = message,
                    onClear = onClear,
                    onPay = onPay,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .aboveTabBar()
                        .enter(appeared, 2),
                )
            }
        }
    }
}

@Composable
private fun ProductCashierRow(
    product: ProductEntity,
    qty: Int,
    scheme: ColorScheme,
    onAdd: (ProductEntity) -> Unit,
    onSetQty: (Long, Int) -> Unit,
) {
    GlassRow(
        title = product.name,
        subtitle = stringResource(
            Res.string.cash_product_line,
            formatRupiah(product.sellPrice),
            product.stock,
        ),
        tint = if (qty > 0) scheme.primaryContainer else scheme.surface,
        alpha = if (qty > 0) 0.95f else 0.88f,
        trailing = {
            if (qty == 0) {
                GlassButton(
                    text = if (product.stock > 0) {
                        stringResource(Res.string.cash_add_cart)
                    } else {
                        stringResource(Res.string.prod_out)
                    },
                    onClick = { onAdd(product) },
                    enabled = product.stock > 0,
                    height = 42.dp,
                )
            } else {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    GlassIconButton(
                        icon = AppIcon.REMOVE,
                        contentDescription = stringResource(Res.string.cd_remove),
                        onClick = { onSetQty(product.id, qty - 1) },
                        size = 38.dp,
                    )
                    Text(
                        "$qty",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium,
                    )
                    GlassIconButton(
                        icon = AppIcon.ADD,
                        contentDescription = stringResource(Res.string.cd_add),
                        onClick = { onSetQty(product.id, qty + 1) },
                        enabled = qty < product.stock,
                        size = 38.dp,
                    )
                }
            }
        },
    )
}

@Composable
private fun LandscapeCartPanel(
    lines: List<CartLine>,
    total: Long,
    message: String,
    cartCount: Int,
    onSetQty: (Long, Int) -> Unit,
    onClear: () -> Unit,
    onPay: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scheme = MaterialTheme.colorScheme
    GlassCard(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        tint = scheme.surface,
        alpha = 0.92f,
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .padding(14.dp),
        ) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Icon(
                        AppIcon.SHOPPING_CART.painter(),
                        contentDescription = null,
                        tint = scheme.primary,
                        modifier = Modifier.size(20.dp),
                    )
                    Text(
                        stringResource(Res.string.tab_cashier),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = scheme.onSurface,
                    )
                }
                if (cartCount > 0) {
                    StatusChip(
                        stringResource(Res.string.cash_cart_items, cartCount),
                        good = true,
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            if (lines.isEmpty()) {
                Box(
                    Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        stringResource(Res.string.cash_empty_t),
                        style = MaterialTheme.typography.bodyMedium,
                        color = scheme.onSurfaceVariant,
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    items(lines, key = { it.product.id }) { line ->
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .glassSurface(
                                    shape = RoundedCornerShape(12.dp),
                                    tint = scheme.surfaceContainerLow,
                                    alpha = 0.65f,
                                    elevation = 2.dp,
                                    specular = 0.15f,
                                    veil = false,
                                )
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(
                                    line.product.name,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                Text(
                                    formatRupiah(line.product.sellPrice * line.qty),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = scheme.primary,
                                    fontWeight = FontWeight.Bold,
                                )
                            }
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                            ) {
                                GlassIconButton(
                                    icon = AppIcon.REMOVE,
                                    contentDescription = stringResource(Res.string.cd_remove),
                                    onClick = { onSetQty(line.product.id, line.qty - 1) },
                                    size = 28.dp,
                                )
                                Text(
                                    "${line.qty}",
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.bodyMedium,
                                )
                                GlassIconButton(
                                    icon = AppIcon.ADD,
                                    contentDescription = stringResource(Res.string.cd_add),
                                    onClick = { onSetQty(line.product.id, line.qty + 1) },
                                    enabled = line.qty < line.product.stock,
                                    size = 28.dp,
                                )
                            }
                        }
                    }
                }
            }

            if (message.isNotBlank()) {
                Text(
                    message,
                    color = scheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(vertical = 4.dp),
                )
            }

            Spacer(Modifier.height(8.dp))

            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    stringResource(Res.string.detail_total),
                    style = MaterialTheme.typography.bodyMedium,
                    color = scheme.onSurfaceVariant,
                )
                Text(
                    formatRupiah(total),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = scheme.primary,
                )
            }

            Spacer(Modifier.height(10.dp))

            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                GlassButton(
                    text = stringResource(Res.string.action_cancel),
                    onClick = onClear,
                    container = scheme.onSurface.copy(alpha = 0.08f),
                    content = scheme.onSurface,
                    modifier = Modifier.weight(1f),
                    height = 44.dp,
                    enabled = lines.isNotEmpty(),
                )
                GlassButton(
                    text = stringResource(Res.string.action_pay),
                    onClick = onPay,
                    modifier = Modifier.weight(1.4f),
                    height = 44.dp,
                    enabled = lines.isNotEmpty(),
                )
            }
        }
    }
}

@Composable
private fun CheckoutCard(
    lines: Int,
    total: Long,
    message: String,
    onClear: () -> Unit,
    onPay: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scheme = MaterialTheme.colorScheme
    Column(
        modifier
            .fillMaxWidth()
            .glassSurface(
                // Radius kontainer HARUS > radius anak. Pill tombol 48dp → radius
                // efektif 24dp, jadi kartu di 28dp. Kalau sama-sama 24dp, tepi
                // pill terlihat terjepit di sudut kartu.
                shape = RoundedCornerShape(GLASS_RADIUS),
                tint = scheme.primary,
                // 0.94 hampir opak: kartu terbaca sebagai blok warna solid dan
                // keluar dari bahasa kaca. 0.90 + gloss lebih kuat supaya tetap
                // satu keluarga dengan kartu lain. Batasnya: di bawah ~0.88
                // konten di belakang mulai bocor dan kartunya jadi kotor.
                alpha = 0.90f,
                elevation = 22.dp,
                specular = 0.34f,
            )
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(
                stringResource(Res.string.cash_cart_items, lines),
                modifier = Modifier.alignByBaseline(),
                color = scheme.onPrimary.copy(alpha = 0.82f),
                style = MaterialTheme.typography.bodyMedium,
            )
            Text(
                formatRupiah(total),
                modifier = Modifier.alignByBaseline(),
                color = scheme.onPrimary,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleLarge,
            )
        }
        if (message.isNotBlank()) {
            Text(
                message,
                color = scheme.onPrimary.copy(alpha = 0.9f),
                style = MaterialTheme.typography.bodySmall,
            )
        }
        // Semua tombol di kartu ini mati selama belum ada produk yang masuk
        // keranjang. Disabled di kartu GELAP = fill lebih GELAP (recessed),
        // label tetap putih penuh: terukur 8.64:1, dan beda 7.37:1 terhadap
        // state enabled (sand) sehingga transisi mati → menyala sangat jelas.
        val barEnabled = lines > 0
        val disabledFill = scheme.onSurface.copy(alpha = 0.30f)
        // Label disabled ikut diredupkan. Sebelumnya putih PENUH di atas fill
        // gelap (9.00:1) — kontrasnya bagus, tapi labelnya jadi terlihat solid
        // sehingga tombolnya terbaca masih aktif. 0.66 → 4.96:1: jelas redup,
        // masih lolos AA. Batasnya 0.60 (4.38:1) — di bawah itu gagal.
        val disabledInk = scheme.onPrimary.copy(alpha = 0.66f)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            // SEKUNDER — ghost: tint tipis 0.10, TANPA gloss dan TANPA bayangan.
            // Sebelumnya 0.16: fill terlalu terang sehingga teks putih cuma
            // 4.54:1 (nyaris gagal AA), dan blob-nya lebih berat dari tombol
            // utama sehingga mata tertarik ke "Batal". Di 0.10 → 5.19:1.
            GlassButton(
                text = stringResource(Res.string.action_cancel),
                onClick = onClear,
                enabled = barEnabled,
                container = scheme.onPrimary.copy(alpha = 0.10f),
                content = scheme.onPrimary,
                elevation = 0.dp,
                specular = 0f,
                disabledContainer = disabledFill,
                disabledContent = disabledInk,
                modifier = Modifier.weight(1f),
            )
            // PRIMER — satu-satunya elemen yang menyala: sand @8.86:1, bergloss,
            // dan paling tinggi. Beda luminance vs ghost 4.42:1 → jelas dominan.
            GlassButton(
                text = stringResource(Res.string.action_pay),
                onClick = onPay,
                enabled = barEnabled,
                container = scheme.secondaryContainer,
                content = scheme.onSecondaryContainer,
                elevation = 14.dp,
                disabledContainer = disabledFill,
                disabledContent = disabledInk,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

/**
 * Dialog pembayaran: nominal, nama pelanggan, dan metode (tunai/transfer/bon).
 * State-nya lokal dan tidak dilaporkan ke atas sampai simpan ditekan, jadi
 * menutup dialog membatalkan isian tanpa efek samping.
 *
 * Tombol simpan tidak pernah dimatikan — kekurangan bayar diberitahukan lewat
 * baris selisih di dalam dialog, bukan dengan mencabut aksinya. Untuk BON
 * nominal bebas: sisanya jadi piutang, itu sebabnya peringatan kekurangan
 * sengaja disembunyikan.
 *
 * @param onConfirm dipanggil dengan (nominal dibayar dalam rupiah utuh, metode,
 *   nama pelanggan); nama pelanggan boleh kosong.
 */
/**
 * Dialog pembayaran: nominal, nama pelanggan, dan metode (tunai/transfer/bon/QRIS).
 * State-nya lokal dan tidak dilaporkan ke atas sampai simpan ditekan, jadi
 * menutup dialog membatalkan isian tanpa efek samping.
 *
 * Pada mode QRIS, ditampilkan barcode QRIS adaptif dengan kontras tinggi (kartu putih)
 * dan tombol berubah menjadi "Konfirmasi Lunas" (AUD-107).
 *
 * @param onConfirm dipanggil dengan (nominal dibayar dalam rupiah utuh, metode,
 *   nama pelanggan); nama pelanggan boleh kosong.
 */
@Composable
fun PayDialog(
    total: Long,
    qrisConfig: QrisConfigEntity? = null,
    initialMethod: String = PayMethod.TUNAI,
    onDismiss: () -> Unit,
    onConfirm: (Long, String, String) -> Unit,
) {
    var paidText by remember { mutableStateOf("") }
    var customer by remember { mutableStateOf("") }
    var method by remember { mutableStateOf(initialMethod) }
    val paid = parseRupiah(paidText)
    val scheme = MaterialTheme.colorScheme
    val isLandscape = LocalIsLandscape.current

    val isQrisConfigured = qrisConfig != null && qrisConfig.isEnabled && qrisConfig.imageBytes != null

    val qrisBitmap: ImageBitmap? = remember(qrisConfig?.imageBytes) {
        qrisConfig?.imageBytes?.let { bytes ->
            try {
                bytes.decodeToImageBitmap()
            } catch (_: Throwable) {
                null
            }
        }
    }

    GlassFormDialog(
        title = stringResource(Res.string.cash_pay_title, formatRupiah(total)),
        onDismiss = onDismiss,
        onConfirm = {
            val finalPaid = if (method == PayMethod.QRIS) total else paid
            onConfirm(finalPaid, method, customer)
        },
        cancelText = stringResource(Res.string.action_close),
        confirmText = if (method == PayMethod.QRIS) "Konfirmasi Lunas" else stringResource(Res.string.action_save),
    ) {
        GlassTextField(
            value = customer,
            onValueChange = { customer = it },
            label = stringResource(Res.string.cash_customer),
            keyboardOptions = KeyboardOptions(
                imeAction = if (method == PayMethod.QRIS) ImeAction.Done else ImeAction.Next,
            ),
        )

        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            GlassChip(
                text = stringResource(Res.string.cash_m_cash),
                selected = method == PayMethod.TUNAI,
                onClick = { method = PayMethod.TUNAI },
            )
            GlassChip(
                text = stringResource(Res.string.cash_m_transfer),
                selected = method == PayMethod.TRANSFER,
                onClick = { method = PayMethod.TRANSFER },
            )
            GlassChip(
                text = stringResource(Res.string.tab_debt),
                selected = method == PayMethod.BON,
                onClick = { method = PayMethod.BON },
            )
            GlassChip(
                text = "QRIS",
                selected = method == PayMethod.QRIS,
                onClick = {
                    if (isQrisConfigured) {
                        method = PayMethod.QRIS
                    }
                },
            )
        }

        if (!isQrisConfigured && method != PayMethod.QRIS) {
            Text(
                text = "QRIS belum aktif. Atur di menu Pengaturan > Pembayaran.",
                style = MaterialTheme.typography.bodySmall,
                color = scheme.onSurfaceVariant.copy(alpha = 0.70f),
            )
        }

        if (method == PayMethod.QRIS) {
            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color.White)
                            .padding(10.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (qrisBitmap != null) {
                            Image(
                                bitmap = qrisBitmap,
                                contentDescription = "Barcode QRIS Pembayaran",
                                contentScale = ContentScale.Fit,
                                // Tinggi diperbesar (potret 380.dp, lanskap 240.dp) dan lebar
                                // mengikuti rasio aspek bitmap agar kode QR tampil besar &
                                // utuh untuk dipindai pelanggan, bukan strip kecil di tengah.
                                modifier = Modifier
                                    .heightIn(max = if (isLandscape) 240.dp else 380.dp),
                            )
                        } else {
                            Text(
                                text = "Gagal memuat gambar barcode QRIS",
                                color = Color.Gray,
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                    }

                    Text(
                        text = "Arahkan kamera pelanggan ke kode QR di atas",
                        style = MaterialTheme.typography.bodySmall,
                        color = scheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                    )
                }
            }

            Row(
                Modifier
                    .fillMaxWidth()
                    .glassSurface(
                        shape = RoundedCornerShape(16.dp),
                        tint = scheme.surfaceContainerLow,
                        alpha = 0.90f,
                        elevation = 6.dp,
                        specular = 0.18f,
                        veil = false,
                    )
                    .padding(14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Total Tagihan",
                    color = scheme.onSurfaceVariant,
                )
                Text(
                    text = formatRupiah(total),
                    fontWeight = FontWeight.Bold,
                    color = scheme.primary,
                    style = MaterialTheme.typography.titleMedium,
                )
            }
        } else {
            GlassTextField(
                value = paidText,
                onValueChange = { input ->
                    val digits = input.filter { it.isDigit() }
                    if (digits.length <= 15) {
                        paidText = digits
                    }
                },
                label = stringResource(Res.string.cash_paid),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Number,
                    imeAction = ImeAction.Done,
                ),
            )

            Row(
                Modifier
                    .fillMaxWidth()
                    .glassSurface(
                        shape = RoundedCornerShape(16.dp),
                        tint = scheme.surfaceContainerLow,
                        alpha = 0.90f,
                        elevation = 6.dp,
                        specular = 0.18f,
                        veil = false,
                    )
                    .padding(14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    if (paid >= total) stringResource(Res.string.cash_change)
                    else stringResource(Res.string.cash_short),
                    color = scheme.onSurfaceVariant,
                )
                Text(
                    formatRupiah(if (paid >= total) paid - total else total - paid),
                    fontWeight = FontWeight.Bold,
                    color = scheme.onSurface,
                )
            }

            if (method != PayMethod.BON && paid < total) {
                Text(
                    stringResource(Res.string.cash_short_hint),
                    color = scheme.error,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }
}

/**
 * Overload pendukung untuk pemanggil tanpa parameter [qrisConfig].
 */
@Composable
fun PayDialog(
    total: Long,
    onDismiss: () -> Unit,
    onConfirm: (Long, String, String) -> Unit,
) = PayDialog(
    total = total,
    qrisConfig = null,
    initialMethod = PayMethod.TUNAI,
    onDismiss = onDismiss,
    onConfirm = onConfirm,
)

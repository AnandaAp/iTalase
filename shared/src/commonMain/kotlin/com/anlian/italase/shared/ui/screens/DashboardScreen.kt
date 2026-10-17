package com.anlian.italase.shared.ui.screens

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.anlian.italase.shared.data.local.dao.TopRow
import com.anlian.italase.shared.data.local.entity.ProductEntity
import com.anlian.italase.shared.data.repository.DaySummary
import com.anlian.italase.shared.ui.components.AppIcon
import com.anlian.italase.shared.ui.components.GlassCard
import com.anlian.italase.shared.ui.components.GlassEmptyState
import com.anlian.italase.shared.ui.components.SectionHeader
import com.anlian.italase.shared.ui.components.painter
import com.anlian.italase.shared.ui.navigation.LocalBottomOverlayPadding
import com.anlian.italase.shared.ui.navigation.LocalTopAppBarScrollBehavior
import com.anlian.italase.shared.ui.navigation.LocalTopOverlayPadding
import com.anlian.italase.shared.ui.navigation.Routes
import com.anlian.italase.shared.ui.navigation.TopBarInfoEffect
import com.anlian.italase.shared.ui.navigation.nestedScrollConnection
import com.anlian.italase.shared.util.formatRupiah
import italase.shared.generated.resources.Res
import italase.shared.generated.resources.dash_debt_running
import italase.shared.generated.resources.dash_hello
import italase.shared.generated.resources.dash_hello_sub
import italase.shared.generated.resources.dash_low
import italase.shared.generated.resources.dash_low_count
import italase.shared.generated.resources.dash_low_empty_h
import italase.shared.generated.resources.dash_low_empty_t
import italase.shared.generated.resources.dash_min_stock
import italase.shared.generated.resources.dash_omzet
import italase.shared.generated.resources.dash_stat_debt
import italase.shared.generated.resources.dash_stat_gross
import italase.shared.generated.resources.dash_stat_net
import italase.shared.generated.resources.dash_stock_left
import italase.shared.generated.resources.dash_top
import italase.shared.generated.resources.dash_top_count
import italase.shared.generated.resources.dash_top_empty_h
import italase.shared.generated.resources.dash_top_empty_t
import italase.shared.generated.resources.dash_top_sold
import italase.shared.generated.resources.dash_trx_profit
import italase.shared.generated.resources.tab_cashier
import italase.shared.generated.resources.tab_debt
import italase.shared.generated.resources.tab_stock
import italase.shared.generated.resources.title_expenses
import org.jetbrains.compose.resources.stringResource

@Composable
fun DashboardScreen(
    summary: DaySummary,
    top: List<TopRow>,
    piutang: Long,
    lowStock: List<ProductEntity>,
    activeDebtCount: Int,
    error: String,
    onRefresh: () -> Unit = {},
    onQuickAction: (String) -> Unit = {},
    // Bertambah tiap kali Beranda dibuka lagi, termasuk setelah pindah tab
    // (destination di-restore, jadi LaunchedEffect(Unit) tidak akan jalan lagi).
    // NavHost yang memegang counter-nya; ringkasan di-refresh oleh call site.
    refreshKey: Int = 0,
) {
    LaunchedEffect(refreshKey) { onRefresh() }

    DashboardContent(
        summary = summary,
        top = top,
        piutang = piutang,
        lowStock = lowStock,
        activeDebtCount = activeDebtCount,
        error = error,
        onQuickAction = onQuickAction,
    )
}

@Composable
fun DashboardContent(
    summary: DaySummary,
    top: List<TopRow>,
    piutang: Long,
    lowStock: List<ProductEntity>,
    activeDebtCount: Int,
    error: String,
    onQuickAction: (String) -> Unit = {},
) {
    val overlayTop = LocalTopOverlayPadding.current
    val overlayBottom = LocalBottomOverlayPadding.current

    // Entry sekali saja per komposisi. Destination di-restore saat pindah tab,
    // jadi `remember` bertahan → animasi tidak diputar ulang tiap balik ke
    // Beranda (yang terasa lambat dan mengganggu).
    var appeared by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { appeared = true }

    val scrollBehavior = LocalTopAppBarScrollBehavior.current
    TopBarInfoEffect(
        eyebrow = stringResource(Res.string.dash_hello_sub),
        title = stringResource(Res.string.dash_hello),
    )
    Column(
        modifier = Modifier
            .fillMaxSize()
            // Urutan penting: nestedScroll WAJIB di luar (sebelum) verticalScroll.
            // Modifier paling awal = paling luar; event scroll naik dari child ke
            // parent, jadi connection harus membungkus scrollable-nya agar
            // contentOffset di top bar benar-benar ter-update.
            .nestedScrollConnection(scrollBehavior, null)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Spacer(Modifier.height(overlayTop + 4.dp))
        if (error.isNotEmpty()) {
            Enter(appeared, 1) {
                Text(
                    error,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }
        }
        Enter(appeared, 1) { OmzetHero(summary) }
        Enter(appeared, 2) { QuickActions(onQuickAction) }
        Enter(appeared, 3) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                GlassStat(
                    label = stringResource(Res.string.dash_stat_net),
                    value = formatRupiah(summary.bersih),
                    modifier = Modifier.weight(1f),
                )
                GlassStat(
                    label = stringResource(Res.string.title_expenses),
                    value = formatRupiah(summary.expense),
                    modifier = Modifier.weight(1f),
                )
            }
        }
        Enter(appeared, 4) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                GlassStat(
                    label = stringResource(Res.string.dash_stat_debt),
                    value = formatRupiah(piutang),
                    modifier = Modifier.weight(1f),
                    sub = stringResource(Res.string.dash_debt_running, activeDebtCount),
                )
                GlassStat(
                    label = stringResource(Res.string.dash_stat_gross),
                    value = formatRupiah(summary.profit),
                    modifier = Modifier.weight(1f),
                )
            }
        }

        Enter(appeared, 5) {
            SectionHeader(
                stringResource(Res.string.dash_top),
                stringResource(Res.string.dash_top_count, top.size),
            )
        }
        Enter(appeared, 6) {
            if (top.isEmpty()) {
                GlassEmptyState(
                    stringResource(Res.string.dash_top_empty_t),
                    stringResource(Res.string.dash_top_empty_h),
                )
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    top.forEachIndexed { i, r -> TopItem(rank = i + 1, row = r) }
                }
            }
        }

        Enter(appeared, 7) {
            SectionHeader(
                stringResource(Res.string.dash_low),
                stringResource(Res.string.dash_low_count, lowStock.size),
            )
        }
        Enter(appeared, 8) {
            if (lowStock.isEmpty()) {
                GlassEmptyState(
                    stringResource(Res.string.dash_low_empty_t),
                    stringResource(Res.string.dash_low_empty_h),
                )
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    lowStock.take(10).forEach { p -> LowStockItem(p) }
                }
            }
        }
        // Ruang gulir ekstra: tarik baris terakhir melewati bar kaca yang
        // mengambang, supaya tidak tertutup permanen di bawahnya.
        Spacer(Modifier.height(overlayBottom))
    }
}

/**
 * Entry bertingkat. Hanya menyentuh `alpha` + `translationY` di dalam
 * graphicsLayer — tidak ada properti layout yang dianimasikan, jadi tidak
 * memicu re-layout/reflow selama animasi berjalan.
 */
@Composable
private fun Enter(
    visible: Boolean,
    index: Int,
    content: @Composable () -> Unit,
) {
    val progress by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(
            durationMillis = 420,
            delayMillis = index * 70,
            // Decelerate: cepat di awal, melambat halus di akhir.
            easing = CubicBezierEasing(0.16f, 1f, 0.3f, 1f),
        ),
        label = "enter$index",
    )
    Box(
        Modifier.graphicsLayer {
            alpha = progress
            translationY = (1f - progress) * 28.dp.toPx()
        },
    ) {
        content()
    }
}

// ── Hero: omzet hari ini ───────────────────────────────────────────────
// Satu-satunya permukaan berwarna kuat di layar. Kaca hijau pekat (α 0.94)
// dengan orb dekoratif — "primary tonal surface" M3 Expressive tanpa gradien.
@Composable
private fun OmzetHero(summary: DaySummary) {
    val scheme = MaterialTheme.colorScheme
    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        tint = scheme.primary,
        alpha = 0.92f,
    ) {
        Box(Modifier.fillMaxWidth()) {
            Box(
                Modifier
                    .size(150.dp).offset(x = 70.dp, y = (-50).dp).align(Alignment.TopEnd)
                    .clip(CircleShape).background(Color.White.copy(alpha = 0.10f))
                    .blur(20.dp),
            )
            Box(
                Modifier
                    .size(96.dp).offset(x = (-24).dp, y = 56.dp).align(Alignment.BottomStart)
                    .clip(CircleShape).background(Color.White.copy(alpha = 0.07f))
                    .blur(16.dp),
            )
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        AppIcon.STOREFRONT.painter(),
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.85f),
                        modifier = Modifier.size(15.dp),
                    )
                    Text(
                        stringResource(Res.string.dash_omzet).uppercase(),
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.85f),
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.9.sp,
                        modifier = Modifier.padding(start = 6.dp),
                    )
                }
                Text(
                    formatRupiah(summary.omzet),
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                )
                Text(
                    stringResource(
                        Res.string.dash_trx_profit,
                        summary.trxCount,
                        formatRupiah(summary.profit),
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.88f),
                )
            }
        }
    }
}

@Composable
private fun QuickActions(onQuickAction: (String) -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        QuickAction(stringResource(Res.string.tab_cashier), AppIcon.SHOPPING_CART, Modifier.weight(1f)) {
            onQuickAction(Routes.CASHIER)
        }
        QuickAction(stringResource(Res.string.tab_stock), AppIcon.INVENTORY, Modifier.weight(1f)) {
            onQuickAction(Routes.STOCK)
        }
        QuickAction(stringResource(Res.string.tab_debt), AppIcon.HANDSHAKE, Modifier.weight(1f)) {
            onQuickAction(Routes.DEBT)
        }
        QuickAction(stringResource(Res.string.title_expenses), AppIcon.PAYMENTS, Modifier.weight(1f)) {
            onQuickAction(Routes.EXPENSE)
        }
    }
}

// Lingkaran kaca 56dp (di atas target sentuh minimum 44/48) + label di bawah.
// Press-scale spring disediakan GlassCard.
@Composable
private fun QuickAction(
    label: String,
    icon: AppIcon,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {},
) {
    val scheme = MaterialTheme.colorScheme
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        GlassCard(
            modifier = Modifier.size(56.dp),
            shape = CircleShape,
            tint = scheme.primaryContainer,
            alpha = 0.60f,
            onClick = onClick,
        ) {
            Icon(
                icon.painter(),
                contentDescription = label,
                tint = scheme.onPrimaryContainer,
                modifier = Modifier.align(Alignment.Center).size(24.dp),
            )
        }
        Spacer(Modifier.height(8.dp))
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            color = scheme.onSurface,
            maxLines = 1,
        )
    }
}

@Composable
private fun GlassStat(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    sub: String = "",
) {
    val scheme = MaterialTheme.colorScheme
    GlassCard(modifier = modifier, shape = RoundedCornerShape(20.dp)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                label.uppercase(),
                style = MaterialTheme.typography.labelSmall,
                color = scheme.onSurfaceVariant,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.8.sp,
                maxLines = 1,
            )
            Text(
                value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = scheme.onSurface,
            )
            if (sub.isNotBlank()) {
                Text(sub, style = MaterialTheme.typography.bodySmall, color = scheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun TopItem(rank: Int, row: TopRow) {
    val scheme = MaterialTheme.colorScheme
    GlassCard(shape = RoundedCornerShape(18.dp)) {
        Row(
            Modifier.fillMaxWidth().padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier.size(34.dp).clip(CircleShape).background(scheme.primaryContainer),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    "$rank",
                    fontWeight = FontWeight.Bold,
                    color = scheme.onPrimaryContainer,
                )
            }
            Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                Text(row.productName, fontWeight = FontWeight.SemiBold)
                Text(
                    stringResource(Res.string.dash_top_sold, row.q),
                    style = MaterialTheme.typography.bodySmall,
                    color = scheme.onSurfaceVariant,
                )
            }
            Text(formatRupiah(row.s), fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun LowStockItem(p: ProductEntity) {
    val scheme = MaterialTheme.colorScheme
    GlassCard(
        shape = RoundedCornerShape(18.dp),
        tint = scheme.secondaryContainer,
        alpha = 0.72f,
    ) {
        Row(
            Modifier.fillMaxWidth().padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(p.name, fontWeight = FontWeight.SemiBold)
                Text(
                    stringResource(Res.string.dash_min_stock, p.minStock, p.unit),
                    style = MaterialTheme.typography.bodySmall,
                    color = scheme.onSecondaryContainer,
                )
            }
            Text(
                stringResource(Res.string.dash_stock_left, p.stock),
                fontWeight = FontWeight.Bold,
                color = scheme.error,
            )
        }
    }
}

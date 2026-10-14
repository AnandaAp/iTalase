package com.anlian.italase.shared.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.anlian.italase.shared.data.local.dao.TopRow
import com.anlian.italase.shared.data.local.entity.ProductEntity
import com.anlian.italase.shared.data.repository.DaySummary
import com.anlian.italase.shared.ui.components.AppIcon
import com.anlian.italase.shared.ui.components.EmptyState
import com.anlian.italase.shared.ui.components.ListCard
import com.anlian.italase.shared.ui.components.ScreenHeader
import com.anlian.italase.shared.ui.components.SectionHeader
import com.anlian.italase.shared.ui.components.painter
import com.anlian.italase.shared.ui.navigation.Routes
import com.anlian.italase.shared.ui.theme.WarungGreenDark
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
) {

    LaunchedEffect(Unit) { onRefresh() }

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
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Spacer(Modifier.height(8.dp))
            ScreenHeader(
                title = stringResource(Res.string.dash_hello),
                subtitle = stringResource(Res.string.dash_hello_sub),
            )
            if (error.isNotEmpty()) {
                Text(
                    error,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }
        }
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.large,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary),
            ) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.horizontalGradient(
                                listOf(MaterialTheme.colorScheme.primary, WarungGreenDark),
                            ),
                        ),
                ) {
                    Box(
                        Modifier.size(140.dp).offset(x = 60.dp, y = (-40).dp).align(Alignment.TopEnd)
                            .clip(CircleShape).background(Color.White.copy(alpha = 0.10f)),
                    )
                    Box(
                        Modifier.size(90.dp).offset(x = (-20).dp, y = 50.dp).align(Alignment.BottomStart)
                            .clip(CircleShape).background(Color.White.copy(alpha = 0.08f)),
                    )
                    Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                AppIcon.STOREFRONT.painter(),
                                contentDescription = null,
                                tint = Color.White.copy(alpha = 0.85f),
                                modifier = Modifier.size(16.dp),
                            )
                            Text(
                                stringResource(Res.string.dash_omzet),
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White.copy(alpha = 0.85f),
                                fontWeight = FontWeight.Bold,
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
                            stringResource(Res.string.dash_trx_profit, summary.trxCount, formatRupiah(summary.profit)),
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.9f),
                        )
                    }
                }
            }
        }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
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
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatCard(
                    stringResource(Res.string.dash_stat_net),
                    formatRupiah(summary.bersih),
                    Modifier.weight(1f),
                )
                StatCard(
                    stringResource(Res.string.title_expenses),
                    formatRupiah(summary.expense),
                    Modifier.weight(1f),
                )
            }
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatCard(
                    stringResource(Res.string.dash_stat_debt),
                    formatRupiah(piutang),
                    Modifier.weight(1f),
                    stringResource(Res.string.dash_debt_running, activeDebtCount),
                )
                StatCard(
                    stringResource(Res.string.dash_stat_gross),
                    formatRupiah(summary.profit),
                    Modifier.weight(1f),
                )
            }
        }
        item {
            SectionHeader(
                stringResource(Res.string.dash_top),
                stringResource(Res.string.dash_top_count, top.size),
            )
        }
        if (top.isEmpty()) {
            item {
                EmptyState(
                    stringResource(Res.string.dash_top_empty_t),
                    stringResource(Res.string.dash_top_empty_h),
                )
            }
        } else {
            items(top, key = { it.productName }) { r ->
                ListCard {
                    Row(
                        Modifier.fillMaxWidth().padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            Modifier.size(36.dp).clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                "${top.indexOf(r) + 1}",
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                            )
                        }
                        Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                            Text(r.productName, fontWeight = FontWeight.SemiBold)
                            Text(
                                stringResource(Res.string.dash_top_sold, r.q),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Text(formatRupiah(r.s), fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
        item {
            SectionHeader(
                stringResource(Res.string.dash_low),
                stringResource(Res.string.dash_low_count, lowStock.size),
            )
        }
        if (lowStock.isEmpty()) {
            item {
                EmptyState(
                    stringResource(Res.string.dash_low_empty_t),
                    stringResource(Res.string.dash_low_empty_h),
                )
            }
        } else {
            items(lowStock.take(10), key = { it.id }) { p ->
                ListCard(tonal = true) {
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
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                            )
                        }
                        Text(
                            stringResource(Res.string.dash_stock_left, p.stock),
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                }
            }
        }
        item { Spacer(Modifier.height(8.dp)) }
    }
}

@Composable
private fun QuickAction(label: String, icon: AppIcon, modifier: Modifier = Modifier, onClick: () -> Unit = {}) {
    FilledTonalButton(
        onClick = onClick,
        modifier = modifier.height(72.dp),
        contentPadding = PaddingValues(4.dp),
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                Modifier.size(28.dp).clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    icon.painter(),
                    contentDescription = label,
                    tint = Color.White,
                    modifier = Modifier.size(18.dp),
                )
            }
            Text(label, style = MaterialTheme.typography.labelSmall, maxLines = 1)
        }
    }
}

@Composable
private fun StatCard(label: String, value: String, modifier: Modifier = Modifier, sub: String = "") {
    ListCard(modifier = modifier) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                label.uppercase(),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Bold,
            )
            Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            if (sub.isNotBlank()) {
                Text(sub, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

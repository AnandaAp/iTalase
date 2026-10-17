package com.anlian.italase.shared.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.anlian.italase.shared.data.local.entity.PayMethod
import com.anlian.italase.shared.data.local.entity.SaleEntity
import com.anlian.italase.shared.data.local.entity.TxnStatus
import com.anlian.italase.shared.ui.components.CategoryChips
import com.anlian.italase.shared.ui.components.EmptyState
import com.anlian.italase.shared.ui.components.ListCard
import com.anlian.italase.shared.ui.components.StatusChip
import com.anlian.italase.shared.ui.navigation.LocalBottomOverlayPadding
import com.anlian.italase.shared.ui.navigation.LocalTopAppBarScrollBehavior
import com.anlian.italase.shared.ui.navigation.LocalTopOverlayPadding
import com.anlian.italase.shared.ui.navigation.TopBarInfoEffect
import com.anlian.italase.shared.ui.navigation.nestedScrollConnection
import com.anlian.italase.shared.util.formatRupiah
import italase.shared.generated.resources.Res
import italase.shared.generated.resources.cash_m_cash
import italase.shared.generated.resources.cash_m_transfer
import italase.shared.generated.resources.hist_count
import italase.shared.generated.resources.hist_empty_h
import italase.shared.generated.resources.hist_empty_t
import italase.shared.generated.resources.hist_profit
import italase.shared.generated.resources.hist_receipt
import italase.shared.generated.resources.status_paid
import italase.shared.generated.resources.tab_debt
import italase.shared.generated.resources.title_history
import org.jetbrains.compose.resources.stringResource

@Composable
fun HistoryScreen(
    sales: List<SaleEntity>,
    categories: List<String>,
    activeCategory: String?,
    onCategory: (String?) -> Unit = {},
    onOpen: (Long) -> Unit = {},
) {
    HistoryContent(sales = sales, categories = categories, activeCategory = activeCategory, onCategory = onCategory, onOpen = onOpen)
}

@Composable
private fun payMethodLabel(method: String): String = when (method) {
    PayMethod.TUNAI -> stringResource(Res.string.cash_m_cash)
    PayMethod.TRANSFER -> stringResource(Res.string.cash_m_transfer)
    else -> stringResource(Res.string.tab_debt)
}

@Composable
fun HistoryContent(
    sales: List<SaleEntity>,
    categories: List<String> = emptyList(),
    activeCategory: String? = null,
    onCategory: (String?) -> Unit = {},
    onOpen: (Long) -> Unit = {},
) {
    val scrollBehavior = LocalTopAppBarScrollBehavior.current
    val listState = rememberLazyListState()
    TopBarInfoEffect(
        eyebrow = stringResource(Res.string.hist_count, sales.size),
        title = stringResource(Res.string.title_history),
    )
    Column(
        Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .nestedScrollConnection(scrollBehavior, listState),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Spacer(Modifier.height(LocalTopOverlayPadding.current + 8.dp))
        CategoryChips(
            categories = categories,
            active = activeCategory,
            onSelect = onCategory,
        )
        if (sales.isEmpty()) {
            EmptyState(
                stringResource(Res.string.hist_empty_t),
                stringResource(Res.string.hist_empty_h),
            )
        } else {
            LazyColumn(state = listState, verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.weight(1f)) {
                items(sales, key = { it.id }) { s ->
                    val lunas = s.status == TxnStatus.LUNAS
                    ListCard(onClick = { onOpen(s.id) }) {
                        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(
                                Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    stringResource(Res.string.hist_receipt, s.id),
                                    fontWeight = FontWeight.Bold,
                                )
                                Text(
                                    formatRupiah(s.total),
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.titleMedium,
                                )
                            }
                            Row(
                                Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Column {
                                    Text(
                                        payMethodLabel(s.payMethod) +
                                            if (s.customerName.isNotBlank()) " • ${s.customerName}" else "",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                    Text(
                                        stringResource(Res.string.hist_profit, formatRupiah(s.profit)),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.SemiBold,
                                    )
                                }
                                StatusChip(
                                    if (lunas) stringResource(Res.string.status_paid)
                                    else stringResource(Res.string.tab_debt),
                                    good = lunas,
                                )
                            }
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(LocalBottomOverlayPadding.current))
    }
}

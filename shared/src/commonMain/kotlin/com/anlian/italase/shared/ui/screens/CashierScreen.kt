package com.anlian.italase.shared.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.anlian.italase.shared.data.local.entity.PayMethod
import com.anlian.italase.shared.data.local.entity.ProductEntity
import com.anlian.italase.shared.data.repository.CartLine
import com.anlian.italase.shared.ui.components.AppIcon
import com.anlian.italase.shared.ui.components.CategoryChips
import com.anlian.italase.shared.ui.components.EmptyState
import com.anlian.italase.shared.ui.components.ListCard
import com.anlian.italase.shared.ui.components.ScreenHeader
import com.anlian.italase.shared.ui.components.painter
import com.anlian.italase.shared.ui.navigation.LocalBottomOverlayPadding
import com.anlian.italase.shared.ui.navigation.LocalTopAppBarScrollBehavior
import com.anlian.italase.shared.ui.navigation.LocalTopOverlayPadding
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
import italase.shared.generated.resources.tab_debt
import italase.shared.generated.resources.title_cashier
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.ImmutableMap
import org.jetbrains.compose.resources.stringResource

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
    onAdd: (ProductEntity) -> Unit = {},
    onSetQty: (Long, Int) -> Unit = { _, _ -> },
    onClear: () -> Unit = {},
    onPay: () -> Unit = {},
    onDismissPay: () -> Unit = {},
    onConfirmPay: (Long, String, String) -> Unit = { _, _, _ -> },
) {
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
        onAdd = onAdd,
        onSetQty = onSetQty,
        onClear = onClear,
        onPay = onPay,
    )

    if (showPayDialog) {
        PayDialog(
            total = total,
            onDismiss = onDismissPay,
            onConfirm = onConfirmPay,
        )
    }
}

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
    onAdd: (ProductEntity) -> Unit = {},
    onSetQty: (Long, Int) -> Unit = { _, _ -> },
    onClear: () -> Unit = {},
    onPay: () -> Unit = {},
) {
    val onDark = Color.White
    val onDarkDim = Color.White.copy(alpha = 0.85f)
    val scrollBehavior = LocalTopAppBarScrollBehavior.current
    val listState = rememberLazyListState()
    val overlayBottom = LocalBottomOverlayPadding.current
    // Layar ini punya checkout card yang mengambang DI ATAS tab bar. Jadi list
    // butuh dua lapis ruang gulir: tinggi tab bar (agar item bisa lewat di
    // belakang tab bar) + tinggi checkout card (agar item terakhir tidak
    // tertutup permanen oleh keduanya).
    val checkoutSpace = overlayBottom + 132.dp + 16.dp

    Box(Modifier.fillMaxSize()) {
    Column(
        Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Spacer(Modifier.height(LocalTopOverlayPadding.current + 8.dp))
        ScreenHeader(
            title = stringResource(Res.string.title_cashier),
            subtitle = stringResource(Res.string.cash_sub),
        )
        OutlinedTextField(
            value = query,
            onValueChange = onQuery,
            label = { Text(stringResource(Res.string.cash_search)) },
            leadingIcon = {
                Icon(
                    AppIcon.SEARCH.painter(),
                    contentDescription = stringResource(Res.string.cd_search),
                )
            },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            shape = MaterialTheme.shapes.medium,
        )
        CategoryChips(
            categories = categories,
            active = activeCategory,
            onSelect = onCategory,
        )
        if (list.isEmpty()) {
            EmptyState(
                stringResource(Res.string.cash_empty_t),
                stringResource(Res.string.cash_empty_h),
            )
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .nestedScrollConnection(scrollBehavior, listState),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                // Ruang ekstra di ekor list: item terakhir bisa digulir ke ATAS
                // tab bar dan checkout card yang mengambang, bukan berhenti
                // tertutup di bawahnya.
                contentPadding = PaddingValues(bottom = checkoutSpace),
            ) {
                items(list.take(60), key = { it.id }) { p ->
                    val qty = cart[p.id] ?: 0
                    if (qty > 0) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                        ) {
                            ProductRow(p, qty, onAdd, onSetQty)
                        }
                    } else {
                        ListCard {
                            ProductRow(p, qty, onAdd, onSetQty)
                        }
                    }
                }
            }
        }
        // Checkout card TIDAK di dalam Column lagi — sekarang overlay yang
        // mengambang di bawah. LazyColumn punya weight(1f) jadi memenuhi
        // seluruh ruang di bawah header, dan checkout card mengambang DI ATAS
        // ekornya. Tab bar yang mengambang berada di bawah checkout card.
    } // Column

        // Overlay checkout: di atas ekor LazyColumn, di atas tab bar.
        Card(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(horizontal = 16.dp)
                // Jeda 16dp di atas tab bar supaya action UI tidak menempel.
                .padding(bottom = overlayBottom + 16.dp)
                .fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary),
            shape = MaterialTheme.shapes.large,
        ) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(
                        stringResource(Res.string.cash_cart_items, lines.size),
                        color = onDarkDim,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Text(
                        formatRupiah(total),
                        color = onDark,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleLarge,
                    )
                }
                if (message.isNotBlank()) {
                    Text(message, color = onDark, style = MaterialTheme.typography.bodySmall)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = onClear,
                        modifier = Modifier.weight(1f).height(48.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = onDark),
                        border = BorderStroke(1.dp, onDarkDim),
                    ) { Text(stringResource(Res.string.action_cancel)) }
                    Button(
                        onClick = onPay,
                        enabled = lines.isNotEmpty(),
                        modifier = Modifier.weight(1f).height(48.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                            disabledContainerColor = onDark.copy(alpha = 0.20f),
                            disabledContentColor = onDark.copy(alpha = 0.65f),
                        ),
                    ) {
                        Text(
                            stringResource(Res.string.action_pay),
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }
        }
    } // Box(Modifier.fillMaxSize())
}

@Composable
private fun ProductRow(
    p: ProductEntity,
    qty: Int,
    onAdd: (ProductEntity) -> Unit,
    onSetQty: (Long, Int) -> Unit,
) {
    Row(
        Modifier.fillMaxWidth().padding(12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(p.name, fontWeight = FontWeight.SemiBold)
            Text(
                stringResource(Res.string.cash_product_line, formatRupiah(p.sellPrice), p.stock),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (qty == 0) {
            Button(onClick = { onAdd(p) }, enabled = p.stock > 0, modifier = Modifier.height(48.dp)) {
                Text(stringResource(Res.string.cash_add_cart))
            }
        } else {
            Stepper(
                qty = qty,
                canPlus = qty < p.stock,
                onMinus = { onSetQty(p.id, qty - 1) },
                onPlus = { onSetQty(p.id, qty + 1) },
            )
        }
    }
}

@Composable
private fun Stepper(qty: Int, canPlus: Boolean, onMinus: () -> Unit, onPlus: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedButton(onClick = onMinus, modifier = Modifier.height(48.dp)) {
            Icon(
                AppIcon.REMOVE.painter(),
                contentDescription = stringResource(Res.string.cd_remove),
            )
        }
        Text("$qty", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
        Button(onClick = onPlus, enabled = canPlus, modifier = Modifier.height(48.dp)) {
            Icon(
                AppIcon.ADD.painter(),
                contentDescription = stringResource(Res.string.cd_add),
            )
        }
    }
}

@Composable
fun PayDialog(total: Long, onDismiss: () -> Unit, onConfirm: (Long, String, String) -> Unit) {
    var paidText by remember { mutableStateOf("") }
    var customer by remember { mutableStateOf("") }
    var method by remember { mutableStateOf(PayMethod.TUNAI) }
    val paid = parseRupiah(paidText)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(Res.string.cash_pay_title, formatRupiah(total))) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    customer, { customer = it },
                    label = { Text(stringResource(Res.string.cash_customer)) }, singleLine = true,
                )
                OutlinedTextField(
                    paidText, { paidText = it },
                    label = { Text(stringResource(Res.string.cash_paid)) }, singleLine = true,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = method == PayMethod.TUNAI,
                        onClick = { method = PayMethod.TUNAI },
                        label = { Text(stringResource(Res.string.cash_m_cash)) },
                    )
                    FilterChip(
                        selected = method == PayMethod.TRANSFER,
                        onClick = { method = PayMethod.TRANSFER },
                        label = { Text(stringResource(Res.string.cash_m_transfer)) },
                    )
                    FilterChip(
                        selected = method == PayMethod.BON,
                        onClick = { method = PayMethod.BON },
                        label = { Text(stringResource(Res.string.tab_debt)) },
                    )
                }
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
                    Row(
                        Modifier.fillMaxWidth().padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            if (paid >= total) stringResource(Res.string.cash_change)
                            else stringResource(Res.string.cash_short),
                        )
                        Text(
                            formatRupiah(if (paid >= total) paid - total else total - paid),
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
                if (method != PayMethod.BON && paid < total) {
                    Text(
                        stringResource(Res.string.cash_short_hint),
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        },
        confirmButton = {
            Button(onClick = { onConfirm(paid, method, customer) }) {
                Text(stringResource(Res.string.action_save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(Res.string.action_close)) }
        },
    )
}

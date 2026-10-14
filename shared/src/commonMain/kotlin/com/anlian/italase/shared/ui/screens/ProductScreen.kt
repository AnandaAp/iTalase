package com.anlian.italase.shared.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.anlian.italase.shared.data.ProductCategories
import com.anlian.italase.shared.data.StockRules
import com.anlian.italase.shared.data.local.entity.ProductEntity
import com.anlian.italase.shared.ui.components.CategoryChips
import com.anlian.italase.shared.ui.components.EmptyState
import com.anlian.italase.shared.ui.components.StatusChip
import com.anlian.italase.shared.ui.components.ListCard
import com.anlian.italase.shared.ui.components.AppIcon
import com.anlian.italase.shared.ui.components.painter
import com.anlian.italase.shared.ui.components.ScreenHeader
import com.anlian.italase.shared.util.formatRupiah
import com.anlian.italase.shared.util.parseRupiah
import italase.shared.generated.resources.Res
import italase.shared.generated.resources.action_add_product
import italase.shared.generated.resources.action_cancel
import italase.shared.generated.resources.action_delete
import italase.shared.generated.resources.action_edit
import italase.shared.generated.resources.action_save
import italase.shared.generated.resources.cd_add_product
import italase.shared.generated.resources.cash_search
import italase.shared.generated.resources.cd_search
import italase.shared.generated.resources.prod_category
import italase.shared.generated.resources.prod_barcode
import italase.shared.generated.resources.prod_buy
import italase.shared.generated.resources.prod_count
import italase.shared.generated.resources.prod_edit_title
import italase.shared.generated.resources.prod_empty_h
import italase.shared.generated.resources.prod_empty_t
import italase.shared.generated.resources.prod_name
import italase.shared.generated.resources.prod_new
import italase.shared.generated.resources.prod_out
import italase.shared.generated.resources.prod_price_line
import italase.shared.generated.resources.prod_sell
import italase.shared.generated.resources.prod_stock
import italase.shared.generated.resources.prod_stock_left
import italase.shared.generated.resources.prod_unit
import italase.shared.generated.resources.title_products
import org.jetbrains.compose.resources.stringResource

@Composable
fun ProductScreen(
    products: List<ProductEntity>,
    query: String,
    onQuery: (String) -> Unit,
    categories: List<String>,
    activeCategory: String?,
    onCategory: (String?) -> Unit,
    message: String,
    showForm: Boolean,
    edit: ProductEntity?,
    onAdd: () -> Unit = {},
    onEdit: (ProductEntity) -> Unit = {},
    onDelete: (ProductEntity) -> Unit = {},
    onDismissForm: () -> Unit = {},
    onSaveForm: (String, Long, Long, Int, String, String, String) -> Unit = { _, _, _, _, _, _, _ -> },
) {
    ProductContent(
        products = products,
        query = query,
        onQuery = onQuery,
        categories = categories,
        activeCategory = activeCategory,
        onCategory = onCategory,
        message = message,
        onAdd = onAdd,
        onEdit = onEdit,
        onDelete = onDelete,
    )

    if (showForm) {
        ProductFormDialog(
            initial = edit,
            onDismiss = onDismissForm,
            onSave = onSaveForm,
        )
    }
}

@Composable
fun ProductContent(
    products: List<ProductEntity>,
    query: String,
    onQuery: (String) -> Unit,
    categories: List<String>,
    activeCategory: String?,
    onCategory: (String?) -> Unit,
    message: String,
    onAdd: () -> Unit = {},
    onEdit: (ProductEntity) -> Unit = {},
    onDelete: (ProductEntity) -> Unit = {},
) {
    Box(Modifier.fillMaxSize()) {
        Column(
            Modifier.fillMaxSize().padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Spacer(Modifier.height(8.dp))
            ScreenHeader(
                title = stringResource(Res.string.title_products),
                subtitle = stringResource(Res.string.prod_count, products.size),
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
            if (message.isNotBlank()) {
                Text(
                    message,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            if (products.isEmpty()) {
                EmptyState(
                    stringResource(Res.string.prod_empty_t),
                    stringResource(Res.string.prod_empty_h),
                )
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(bottom = 88.dp),
                ) {
                    items(products, key = { it.id }) { p ->
                        val low = p.stock <= p.minStock || p.stock <= StockRules.LOW_STOCK_FALLBACK
                        ListCard(tonal = low) {
                            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text(p.name, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                                    Text(
                                        if (p.stock == 0) stringResource(Res.string.prod_out)
                                        else stringResource(Res.string.prod_stock_left, p.stock),
                                        fontWeight = FontWeight.Bold,
                                        color = if (low) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                                    )
                                }
                                if (p.category.isNotBlank()) {
                                    StatusChip(p.category, good = true)
                                }
                                Text(
                                    stringResource(
                                        Res.string.prod_price_line,
                                        formatRupiah(p.buyPrice),
                                        formatRupiah(p.sellPrice),
                                        formatRupiah(p.sellPrice - p.buyPrice),
                                        p.unit,
                                    ),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                Row {
                                    TextButton(onClick = { onEdit(p) }) {
                                        Icon(
                                            AppIcon.EDIT.painter(),
                                            contentDescription = null,
                                            modifier = Modifier.size(18.dp),
                                        )
                                        Text(
                                            stringResource(Res.string.action_edit),
                                            modifier = Modifier.padding(start = 4.dp),
                                        )
                                    }
                                    TextButton(onClick = { onDelete(p) }) {
                                        Icon(
                                            AppIcon.DELETE.painter(),
                                            contentDescription = null,
                                            modifier = Modifier.size(18.dp),
                                        )
                                        Text(
                                            stringResource(Res.string.action_delete),
                                            modifier = Modifier.padding(start = 4.dp),
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
        }
        ExtendedFloatingActionButton(
            onClick = onAdd,
            modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp),
            text = { Text(stringResource(Res.string.action_add_product)) },
            icon = {
                Icon(
                    AppIcon.ADD.painter(),
                    contentDescription = stringResource(Res.string.cd_add_product),
                )
            },
        )
    }
}

@Composable
fun ProductFormDialog(
    initial: ProductEntity?,
    onDismiss: () -> Unit,
    onSave: (String, Long, Long, Int, String, String, String) -> Unit,
) {
    var name by remember(initial) { mutableStateOf(initial?.name ?: "") }
    var buy by remember(initial) { mutableStateOf(initial?.buyPrice?.toString() ?: "") }
    var sell by remember(initial) { mutableStateOf(initial?.sellPrice?.toString() ?: "") }
    var stock by remember(initial) { mutableStateOf(initial?.stock?.toString() ?: "") }
    var unit by remember(initial) { mutableStateOf(initial?.unit ?: "") }
    var barcode by remember(initial) { mutableStateOf(initial?.barcode ?: "") }
    var category by remember(initial) { mutableStateOf(initial?.category ?: ProductCategories.OTHER) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                if (initial == null) stringResource(Res.string.prod_new)
                else stringResource(Res.string.prod_edit_title),
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    name, { name = it },
                    label = { Text(stringResource(Res.string.prod_name)) }, singleLine = true,
                )
                Text(
                    stringResource(Res.string.prod_category),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Bold,
                )
                @OptIn(ExperimentalLayoutApi::class)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ProductCategories.ALL.forEach { option ->
                        FilterChip(
                            selected = category == option,
                            onClick = { category = option },
                            label = { Text(option) },
                        )
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        buy, { buy = it },
                        label = { Text(stringResource(Res.string.prod_buy)) },
                        singleLine = true, modifier = Modifier.weight(1f),
                    )
                    OutlinedTextField(
                        sell, { sell = it },
                        label = { Text(stringResource(Res.string.prod_sell)) },
                        singleLine = true, modifier = Modifier.weight(1f),
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        stock, { stock = it },
                        label = { Text(stringResource(Res.string.prod_stock)) },
                        singleLine = true, modifier = Modifier.weight(1f),
                    )
                    OutlinedTextField(
                        unit, { unit = it },
                        label = { Text(stringResource(Res.string.prod_unit)) },
                        singleLine = true, modifier = Modifier.weight(1f),
                    )
                }
                OutlinedTextField(
                    barcode, { barcode = it },
                    label = { Text(stringResource(Res.string.prod_barcode)) }, singleLine = true,
                )
            }
        },
        confirmButton = {
            Button(onClick = {
                onSave(name, parseRupiah(buy), parseRupiah(sell), stock.toIntOrNull() ?: 0, unit, barcode, category)
            }) { Text(stringResource(Res.string.action_save)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(Res.string.action_cancel)) }
        },
    )
}

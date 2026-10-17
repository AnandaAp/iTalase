package com.anlian.italase.shared.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
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
import com.anlian.italase.shared.data.local.entity.ExpenseEntity
import com.anlian.italase.shared.ui.components.AnnounceGlassDialogOpen
import com.anlian.italase.shared.ui.components.AppIcon
import com.anlian.italase.shared.ui.components.EmptyState
import com.anlian.italase.shared.ui.components.PrimaryFab
import com.anlian.italase.shared.ui.components.enter
import com.anlian.italase.shared.ui.components.painter
import com.anlian.italase.shared.ui.components.rememberAppear
import com.anlian.italase.shared.ui.navigation.ACTION_FAB_HEIGHT
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
import italase.shared.generated.resources.action_delete
import italase.shared.generated.resources.action_record
import italase.shared.generated.resources.action_save
import italase.shared.generated.resources.cd_add_expense
import italase.shared.generated.resources.debt_note
import italase.shared.generated.resources.exp_amount
import italase.shared.generated.resources.exp_category
import italase.shared.generated.resources.exp_count
import italase.shared.generated.resources.exp_empty_h
import italase.shared.generated.resources.exp_empty_t
import italase.shared.generated.resources.exp_form
import italase.shared.generated.resources.exp_name
import italase.shared.generated.resources.exp_total
import italase.shared.generated.resources.title_expenses
import org.jetbrains.compose.resources.stringResource
import androidx.compose.foundation.shape.RoundedCornerShape
import com.anlian.italase.shared.ui.components.GlassCard
import com.anlian.italase.shared.ui.components.LoadMoreFooter
import com.anlian.italase.shared.ui.components.onBottomReached

@Composable
fun ExpenseScreen(
    items: List<ExpenseEntity>,
    message: String,
    showAddDialog: Boolean,
    loadingMore: Boolean = false,
    hasMore: Boolean = false,
    onAdd: () -> Unit = {},
    onDelete: (Long) -> Unit = {},
    onDismissAdd: () -> Unit = {},
    onSaveExpense: (String, String, Long, String) -> Unit = { _, _, _, _ -> },
    onLoadMore: () -> Unit = {},
) {
    ExpenseContent(
        items = items,
        message = message,
        loadingMore = loadingMore,
        hasMore = hasMore,
        onAdd = onAdd,
        onDelete = onDelete,
        onLoadMore = onLoadMore,
    )

    if (showAddDialog) {
        ExpenseFormDialog(
            onDismiss = onDismissAdd,
            onSave = onSaveExpense,
        )
    }
}

@Composable
fun ExpenseContent(
    items: List<ExpenseEntity>,
    message: String,
    loadingMore: Boolean = false,
    hasMore: Boolean = false,
    onAdd: () -> Unit = {},
    onDelete: (Long) -> Unit = {},
    onLoadMore: () -> Unit = {},
) {
    Box(Modifier.fillMaxSize()) {
        val scrollBehavior = LocalTopAppBarScrollBehavior.current
        val listState = rememberLazyListState()
        val appeared = rememberAppear()
        TopBarInfoEffect(
            eyebrow = stringResource(Res.string.exp_count, items.size),
            title = stringResource(Res.string.title_expenses),
        )
        Column(
            Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
                .nestedScrollConnection(scrollBehavior, listState),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Spacer(Modifier.height(LocalTopOverlayPadding.current + 8.dp))
            // Kartu total: primitif kaca yang sama dengan layar lain, warna
            // maknanya tetap (sand) — lihat DebtScreen untuk alasannya.
            GlassCard(
                modifier = Modifier.fillMaxWidth().enter(appeared, 0),
                tint = MaterialTheme.colorScheme.secondaryContainer,
                alpha = 0.95f,
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text(
                        stringResource(Res.string.exp_total),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.85f),
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        formatRupiah(items.sumOf { it.amount }),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                    )
                }
            }
            if (message.isNotBlank()) {
                Text(
                    message,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            if (items.isEmpty()) {
                EmptyState(
                    stringResource(Res.string.exp_empty_t),
                    stringResource(Res.string.exp_empty_h),
                )
            } else {
                // Lihat catatan di HistoryScreen: isi LazyColumn bukan scope
                // @Composable, jadi pemicunya dipasang di luar.
                listState.onBottomReached(
                    enabled = hasMore && !loadingMore,
                    onLoadMore = onLoadMore,
                )
                LazyColumn(
                    state = listState,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f).enter(appeared, 1),
                    contentPadding = bottomActionTailPadding(ACTION_FAB_HEIGHT),
                ) {
                    items(items, key = { it.id }) { e ->
                        GlassCard(shape = RoundedCornerShape(20.dp)) {
                            Row(
                                Modifier.fillMaxWidth().padding(14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Column(Modifier.weight(1f)) {
                                    Text(e.name, fontWeight = FontWeight.SemiBold)
                                    Text(
                                        e.category + if (e.note.isNotBlank()) " • ${e.note}" else "",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(formatRupiah(e.amount), fontWeight = FontWeight.Bold)
                                    TextButton(onClick = { onDelete(e.id) }) {
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
                    item(key = "load-more") { LoadMoreFooter(loadingMore) }
                }
            }
        }
        PrimaryFab(
            text = stringResource(Res.string.action_record),
            icon = AppIcon.ADD,
            contentDescription = stringResource(Res.string.cd_add_expense),
            onClick = onAdd,
            modifier = Modifier.align(Alignment.BottomEnd).aboveTabBar().enter(appeared, 2),
        )
    }
}

@Composable
fun ExpenseFormDialog(onDismiss: () -> Unit, onSave: (String, String, Long, String) -> Unit) {
    var name by remember { mutableStateOf("") }
    var cat by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    AnnounceGlassDialogOpen()
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(Res.string.exp_form)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    name, { name = it },
                    label = { Text(stringResource(Res.string.exp_name)) }, singleLine = true,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        cat, { cat = it },
                        label = { Text(stringResource(Res.string.exp_category)) },
                        singleLine = true, modifier = Modifier.weight(1f),
                    )
                    OutlinedTextField(
                        amount, { amount = it },
                        label = { Text(stringResource(Res.string.exp_amount)) },
                        singleLine = true, modifier = Modifier.weight(1f),
                    )
                }
                OutlinedTextField(
                    note, { note = it },
                    label = { Text(stringResource(Res.string.debt_note)) }, singleLine = true,
                )
            }
        },
        confirmButton = {
            Button(onClick = { onSave(name, cat, parseRupiah(amount), note) }) {
                Text(stringResource(Res.string.action_save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(Res.string.action_cancel)) }
        },
    )
}

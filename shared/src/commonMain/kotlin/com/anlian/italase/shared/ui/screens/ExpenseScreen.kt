package com.anlian.italase.shared.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import com.anlian.italase.shared.ui.components.GlassButton
import com.anlian.italase.shared.ui.components.GlassDialog
import com.anlian.italase.shared.ui.components.GlassFormDialog
import com.anlian.italase.shared.ui.components.GlassTextField
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
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
import com.anlian.italase.shared.ui.components.GlassEmptyState
import com.anlian.italase.shared.ui.components.GlassShimmerCard
import com.anlian.italase.shared.ui.components.ShimmerCrossfade
import com.anlian.italase.shared.ui.components.GlassCard
import com.anlian.italase.shared.ui.components.LoadMoreFooter
import com.anlian.italase.shared.ui.components.StatusChip
import com.anlian.italase.shared.ui.components.OnBottomReached
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

/**
 * Layar pengeluaran (kas keluar) beserta dialog catatnya.
 *
 * `items` adalah halaman yang sudah dimuat, bukan seluruh riwayat — [hasMore],
 * [loadingMore], dan [onLoadMore] meneruskan kontrak memuat-bertahap milik
 * ViewModel; `onLoadMore` boleh dipanggil berkali-kali selama `hasMore` masih
 * true, jadi implementasinya harus idempoten terhadap pemanggilan berulang.
 */
@Composable
fun ExpenseScreen(
    items: List<ExpenseEntity>,
    message: String,
    showAddDialog: Boolean,
    loadingMore: Boolean = false,
    hasMore: Boolean = false,
    isLoading: Boolean = false,
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
        isLoading = isLoading,
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

/**
 * Bagian daftar pengeluaran tanpa dialog; dipakai layar penuh maupun preview.
 *
 * Observes: scroll behavior top bar (judul/eyebrow), `rememberAppear` untuk animasi
 * masuk, dan `listState` untuk pemicu muat-tambahan. Total di kartu atas dijumlah
 * dari item yang sedang dipegang, jadi angkanya ikut bertambah setiap halaman baru
 * masuk — bukan total keseluruhan.
 */
@Composable
fun ExpenseContent(
    items: List<ExpenseEntity>,
    message: String,
    loadingMore: Boolean = false,
    hasMore: Boolean = false,
    isLoading: Boolean = false,
    onAdd: () -> Unit = {},
    onDelete: (Long) -> Unit = {},
    onLoadMore: () -> Unit = {},
) {
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val isLandscape = (maxWidth > maxHeight && maxWidth >= 540.dp) || maxWidth >= 600.dp
        val scrollBehavior = LocalTopAppBarScrollBehavior.current
        val listState = rememberLazyListState()
        val appeared = rememberAppear(!isLoading)
        val totalAmount = remember(items) { items.sumOf { it.amount } }
        TopBarInfoEffect(
            eyebrow = stringResource(Res.string.exp_count, items.size),
            title = stringResource(Res.string.title_expenses),
        )

        if (isLandscape) {
            // Tata letak Landscape POS: Split screen Master-Detail
            // Kiri: Kartu Ringkasan Total Beban & Status, Kanan: Daftar Catatan Beban
            Row(
                Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Column(
                    Modifier
                        .weight(0.38f)
                        .fillMaxHeight(),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Spacer(Modifier.height(LocalTopOverlayPadding.current + 8.dp))
                    ShimmerCrossfade(
                        loading = isLoading,
                        modifier = Modifier.fillMaxWidth(),
                        shimmer = {
                            GlassShimmerCard(height = 110.dp)
                        },
                        content = {
                            GlassCard(
                                modifier = Modifier.fillMaxWidth().enter(appeared, 0),
                                tint = MaterialTheme.colorScheme.secondaryContainer,
                                alpha = 0.95f,
                            ) {
                                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text(
                                        stringResource(Res.string.exp_total),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.85f),
                                        fontWeight = FontWeight.Bold,
                                    )
                                    Text(
                                        formatRupiah(totalAmount),
                                        style = MaterialTheme.typography.headlineMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                                    )
                                    Spacer(Modifier.height(4.dp))
                                    StatusChip(
                                        text = stringResource(Res.string.exp_count, items.size),
                                        good = true,
                                    )
                                }
                            }
                        }
                    )
                    if (message.isNotBlank()) {
                        Text(
                            message,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                }

                Column(
                    Modifier
                        .weight(0.62f)
                        .fillMaxHeight()
                        .nestedScrollConnection(scrollBehavior, listState),
                ) {
                    Spacer(Modifier.height(LocalTopOverlayPadding.current + 8.dp))
                    ShimmerCrossfade(
                        loading = isLoading,
                        modifier = Modifier.fillMaxSize(),
                        shimmer = {
                            Column(
                                modifier = Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                repeat(4) { GlassShimmerCard(height = 76.dp) }
                            }
                        },
                        content = {
                            if (items.isEmpty()) {
                                GlassEmptyState(
                                    stringResource(Res.string.exp_empty_t),
                                    stringResource(Res.string.exp_empty_h),
                                )
                            } else {
                                listState.OnBottomReached(
                                    enabled = hasMore && !loadingMore,
                                    onLoadMore = onLoadMore,
                                )
                                LazyColumn(
                                    state = listState,
                                    verticalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.fillMaxSize(),
                                    contentPadding = bottomActionTailPadding(ACTION_FAB_HEIGHT),
                                ) {
                                    items(items, key = { it.id }) { e ->
                                        ExpenseItemCard(e = e, onDelete = onDelete)
                                    }
                                    item(key = "load-more") { LoadMoreFooter(loadingMore) }
                                }
                            }
                        }
                    )
                }
            }
        } else {
            Column(
                Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp)
                    .nestedScrollConnection(scrollBehavior, listState),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Spacer(Modifier.height(LocalTopOverlayPadding.current + 8.dp))
                ShimmerCrossfade(
                    loading = isLoading,
                    modifier = Modifier.fillMaxWidth(),
                    shimmer = {
                        GlassShimmerCard(height = 110.dp)
                    },
                    content = {
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
                                    formatRupiah(totalAmount),
                                    style = MaterialTheme.typography.headlineMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                                )
                            }
                        }
                    }
                )
                if (message.isNotBlank()) {
                    Text(
                        message,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
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
                            repeat(4) { GlassShimmerCard(height = 76.dp) }
                        }
                    },
                    content = {
                        if (items.isEmpty()) {
                            GlassEmptyState(
                                stringResource(Res.string.exp_empty_t),
                                stringResource(Res.string.exp_empty_h),
                            )
                        } else {
                            // Lihat catatan di HistoryScreen: isi LazyColumn bukan scope
                            // @Composable, jadi pemicunya dipasang di luar.
                            listState.OnBottomReached(
                                enabled = hasMore && !loadingMore,
                                onLoadMore = onLoadMore,
                            )
                            LazyColumn(
                                state = listState,
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = bottomActionTailPadding(ACTION_FAB_HEIGHT),
                            ) {
                                items(items, key = { it.id }) { e ->
                                    ExpenseItemCard(e = e, onDelete = onDelete)
                                }
                                item(key = "load-more") { LoadMoreFooter(loadingMore) }
                            }
                        }
                    }
                )
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
private fun ExpenseItemCard(
    e: ExpenseEntity,
    onDelete: (Long) -> Unit,
) {
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

/**
 * Dialog catat pengeluaran baru.
 *
 * Empat isian: nama, kategori, nominal, catatan. Nominal diketik sebagai teks bebas
 * dan baru dinormalkan `parseRupiah` saat simpan, sama seperti form uang lain di app
 * ini. Kategori dan nominal disejajarkan sebaris karena keduanya pendek.
 */
/**
 * Dialog pencatatan pengeluaran baru dengan bahasa liquid glass.
 */
@Composable
fun ExpenseFormDialog(onDismiss: () -> Unit, onSave: (String, String, Long, String) -> Unit) {
    var name by remember { mutableStateOf("") }
    var cat by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }

    GlassFormDialog(
        title = stringResource(Res.string.exp_form),
        onDismiss = onDismiss,
        onConfirm = { onSave(name, cat, parseRupiah(amount), note) },
        confirmEnabled = name.isNotBlank() && amount.isNotBlank(),
    ) {
        GlassTextField(
            value = name,
            onValueChange = { name = it },
            label = stringResource(Res.string.exp_name),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            GlassTextField(
                value = cat,
                onValueChange = { cat = it },
                label = stringResource(Res.string.exp_category),
                modifier = Modifier.weight(1f),
            )
            GlassTextField(
                value = amount,
                onValueChange = { amount = it },
                label = stringResource(Res.string.exp_amount),
                modifier = Modifier.weight(1f),
            )
        }
        GlassTextField(
            value = note,
            onValueChange = { note = it },
            label = stringResource(Res.string.debt_note),
        )
    }
}

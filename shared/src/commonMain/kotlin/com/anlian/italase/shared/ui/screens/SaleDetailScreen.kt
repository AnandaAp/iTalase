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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import com.anlian.italase.shared.data.local.entity.PayMethod
import com.anlian.italase.shared.data.local.entity.TxnStatus
import com.anlian.italase.shared.data.repository.ShopRepository.SaleDetail
import com.anlian.italase.shared.ui.components.AnnounceGlassDialogOpen
import com.anlian.italase.shared.ui.components.AppIcon
import com.anlian.italase.shared.ui.components.EmptyState
import com.anlian.italase.shared.ui.components.GlassCard
import com.anlian.italase.shared.ui.components.PrimaryFab
import com.anlian.italase.shared.ui.components.SectionHeader
import com.anlian.italase.shared.ui.components.StatusChip
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
import com.anlian.italase.shared.util.formatDateTime
import com.anlian.italase.shared.util.formatRupiah
import italase.shared.generated.resources.Res
import italase.shared.generated.resources.action_cancel
import italase.shared.generated.resources.action_edit
import italase.shared.generated.resources.action_save
import italase.shared.generated.resources.cash_m_cash
import italase.shared.generated.resources.cash_m_transfer
import italase.shared.generated.resources.debt_name
import italase.shared.generated.resources.debt_note
import italase.shared.generated.resources.detail_back
import italase.shared.generated.resources.detail_change
import italase.shared.generated.resources.detail_edit_title
import italase.shared.generated.resources.detail_item_line
import italase.shared.generated.resources.detail_items
import italase.shared.generated.resources.detail_loading
import italase.shared.generated.resources.detail_not_found
import italase.shared.generated.resources.detail_received
import italase.shared.generated.resources.detail_remaining
import italase.shared.generated.resources.detail_total
import italase.shared.generated.resources.hist_profit
import italase.shared.generated.resources.hist_receipt
import italase.shared.generated.resources.status_paid
import italase.shared.generated.resources.tab_debt
import italase.shared.generated.resources.title_detail
import org.jetbrains.compose.resources.stringResource

/**
 * Layar detail satu nota penjualan.
 *
 * Seluruh state diangkat ke pemanggil (NavHost): layar tidak memuat data sendiri,
 * hanya merender yang dikirim dan meneruskan aksi lewat callback. `detail` null
 * berarti "belum selesai dimuat", bukan "tidak ada" — karena itu pesan loading dan
 * not-found ditampilkan sekaligus, sedangkan FAB Edit dan dialog edit baru muncul
 * ketika datanya sudah ada. Kalau tidak, menekan Edit pada nota yang belum termuat
 * akan membuka dialog dengan isian kosong.
 */
@Composable
fun SaleDetailScreen(
    detail: SaleDetail?,
    message: String,
    showEditDialog: Boolean,
    onBack: () -> Unit = {},
    onEdit: () -> Unit = {},
    onDismissEdit: () -> Unit = {},
    onSaveEdit: (String, String) -> Unit = { _, _ -> },
) {
    Box(Modifier.fillMaxSize()) {
        // FAB di sini milik composable ini; list-nya ada di SaleDetailContent
        // yang punya flag sendiri. Masing-masing scope komposisi, jadi tidak bisa
        // berbagi `remember`.
        val appeared = rememberAppear()
        Column(
            Modifier.fillMaxSize().padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Spacer(Modifier.height(LocalTopOverlayPadding.current + 8.dp))
            TextButton(onClick = onBack) {
                Icon(
                    AppIcon.ARROW_BACK.painter(),
                    contentDescription = stringResource(Res.string.detail_back),
                )
                Text(
                    stringResource(Res.string.detail_back),
                    modifier = Modifier.padding(start = 4.dp),
                )
            }
            if (detail == null) {
                EmptyState(
                    stringResource(Res.string.detail_loading),
                    stringResource(Res.string.detail_not_found),
                )
            } else {
                SaleDetailContent(detail = detail, message = message)
            }
        }
        if (detail != null) {
            PrimaryFab(
                text = stringResource(Res.string.action_edit),
                icon = AppIcon.EDIT,
                contentDescription = stringResource(Res.string.detail_edit_title),
                onClick = onEdit,
                modifier = Modifier.align(Alignment.BottomEnd).aboveTabBar().enter(appeared, 2),
            )
        }
    }

    if (showEditDialog && detail != null) {
        SaleEditDialog(
            initialCustomer = detail.sale.customerName,
            initialNote = detail.sale.note,
            onDismiss = onDismissEdit,
            onSave = onSaveEdit,
        )
    }
}

/**
 * Isi nota: identitas pelanggan, daftar barang, lalu ringkasan pembayaran.
 *
 * Header struk tidak dirender di sini tetapi ditulis ke top bar lewat
 * [TopBarInfoEffect] agar tidak dobel. Prasyarat scope: `LocalTopAppBarScrollBehavior`
 * dan `LocalTopOverlayPadding` harus tersedia (disiapkan ScreenScope di AppNavHost).
 *
 * Catatan nominal: baris Diterima/Kembalian hanya muncul bila `tendered` terisi.
 * Nota lama menyimpan null di sana karena nominalnya memang tidak pernah dicatat —
 * menampilkannya sebagai 0 akan terbaca sebagai "pelanggan bayar nol".
 */
@Composable
private fun SaleDetailContent(detail: SaleDetail, message: String) {
    val sale = detail.sale
    val lunas = sale.status == TxnStatus.LUNAS
    val tendered: Long = sale.tendered ?: sale.paidAmount
    val change: Long = tendered - sale.total
    val remaining: Long = (sale.total - sale.paidAmount).coerceAtLeast(0)
    val methodLabel = when (sale.payMethod) {
        PayMethod.TUNAI -> stringResource(Res.string.cash_m_cash)
        PayMethod.TRANSFER -> stringResource(Res.string.cash_m_transfer)
        else -> stringResource(Res.string.tab_debt)
    }

    val scrollBehavior = LocalTopAppBarScrollBehavior.current
    val listState = rememberLazyListState()
    // Header struk dipindah ke top bar supaya tidak dobel. Diletakkan di sini
    // (di luar LazyColumn) agar nilainya tetap tertulis walau item list
    // ter-recycle saat menggulir.
    val appeared = rememberAppear()
    TopBarInfoEffect(
        eyebrow = formatDateTime(sale.dateMillis),
        title = stringResource(Res.string.hist_receipt, sale.id),
    )
    Column(
        Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
    LazyColumn(
        state = listState,
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier
            .weight(1f)
            .nestedScrollConnection(scrollBehavior, listState).enter(appeared, 1),
        // Ruang ekor: baris terakhir bisa digulir ke atas tab bar.
        contentPadding = bottomActionTailPadding(ACTION_FAB_HEIGHT),
    ) {
        item {
            GlassCard(shape = RoundedCornerShape(20.dp)) {
                Row(
                    Modifier.fillMaxWidth().padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            if (sale.customerName.isNotBlank()) sale.customerName else methodLabel,
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleMedium,
                        )
                        Text(
                            stringResource(Res.string.title_detail),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
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
        if (message.isNotBlank()) {
            item {
                Text(
                    message,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
        item {
            SectionHeader(stringResource(Res.string.detail_items))
        }
        items(detail.items, key = { it.id }) { item ->
            GlassCard(shape = RoundedCornerShape(20.dp)) {
                Row(
                    Modifier.fillMaxWidth().padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(item.productName, fontWeight = FontWeight.SemiBold)
                        Text(
                            stringResource(
                                Res.string.detail_item_line,
                                item.qty,
                                formatRupiah(item.sellPrice),
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        if (item.category.isNotBlank()) {
                            Text(
                                item.category,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    }
                    Text(formatRupiah(item.subtotal), fontWeight = FontWeight.Bold)
                }
            }
        }
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary),
                shape = MaterialTheme.shapes.large,
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    PaymentRow(
                        stringResource(Res.string.detail_total),
                        formatRupiah(sale.total),
                        emphasis = true,
                    )
                    // Struk lama (tendered null) tidak menampilkan Diterima/Kembalian
                    // karena nominalnya memang tidak tercatat saat itu.
                    val hasTendered: Boolean = sale.tendered != null
                    if (hasTendered) {
                        PaymentRow(
                            "$methodLabel • ${stringResource(Res.string.detail_received)}",
                            formatRupiah(tendered),
                            emphasis = false,
                        )
                    }
                    if (hasTendered && change > 0) {
                        PaymentRow(
                            stringResource(Res.string.detail_change),
                            formatRupiah(change),
                            emphasis = true,
                        )
                    }
                    if (!lunas && remaining > 0) {
                        PaymentRow(
                            stringResource(Res.string.detail_remaining),
                            formatRupiah(remaining),
                            emphasis = true,
                        )
                    }
                    Text(
                        stringResource(Res.string.hist_profit, formatRupiah(sale.profit)),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f),
                    )
                }
            }
        }
        if (sale.note.isNotBlank()) {
            item {
                GlassCard(shape = RoundedCornerShape(20.dp)) {
                    Column(Modifier.padding(14.dp)) {
                        Text(
                            stringResource(Res.string.debt_note),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(sale.note, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }
    }
    }
}

/**
 * Satu baris label/nilai pada kartu ringkasan pembayaran.
 *
 * Warna teks di-hardcode ke `onPrimary` karena baris ini tidak pernah keluar dari
 * kartu ber-tint primary di atasnya. `emphasis` menaikkan bobot dan ukuran font
 * untuk angka yang paling perlu terbaca cepat (total, kembalian, sisa).
 */
@Composable
private fun PaymentRow(
    label: String,
    value: String,
    emphasis: Boolean,
) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(
            label,
            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f),
            style = MaterialTheme.typography.bodyMedium,
        )
        Text(
            value,
            color = MaterialTheme.colorScheme.onPrimary,
            fontWeight = if (emphasis) FontWeight.Bold else FontWeight.SemiBold,
            style = if (emphasis) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyMedium,
        )
    }
}

/**
 * Dialog ubah nama pelanggan dan catatan sebuah nota.
 *
 * Isian di-seed dari [initialCustomer]/[initialNote] lewat `remember(key)`, jadi
 * nilai awal yang berubah akan menyetel ulang yang sedang diketik — pemanggil
 * wajib menutup dialog setelah simpan agar tidak ada editan setengah jalan yang
 * tertimpa. Dipanggil SaleDetailScreen hanya saat `showEditDialog` true.
 */
@Composable
fun SaleEditDialog(
    initialCustomer: String,
    initialNote: String,
    onDismiss: () -> Unit,
    onSave: (String, String) -> Unit,
) {
    var customer by remember(initialCustomer) { mutableStateOf(initialCustomer) }
    var note by remember(initialNote) { mutableStateOf(initialNote) }
    AnnounceGlassDialogOpen()
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(Res.string.detail_edit_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    customer, { customer = it },
                    label = { Text(stringResource(Res.string.debt_name)) }, singleLine = true,
                )
                OutlinedTextField(
                    note, { note = it },
                    label = { Text(stringResource(Res.string.debt_note)) }, singleLine = true,
                )
            }
        },
        confirmButton = {
            Button(onClick = { onSave(customer, note) }) {
                Text(stringResource(Res.string.action_save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(Res.string.action_cancel)) }
        },
    )
}

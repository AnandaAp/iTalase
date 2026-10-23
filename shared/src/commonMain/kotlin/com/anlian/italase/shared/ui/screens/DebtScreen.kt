package com.anlian.italase.shared.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import com.anlian.italase.shared.ui.components.GlassDialog
import com.anlian.italase.shared.ui.components.GlassTextField
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
import androidx.compose.material3.LinearProgressIndicator
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
import com.anlian.italase.shared.data.local.entity.DebtEntity
import com.anlian.italase.shared.data.local.entity.DebtStatus
import com.anlian.italase.shared.ui.components.AnnounceGlassDialogOpen
import com.anlian.italase.shared.ui.components.AppIcon
import com.anlian.italase.shared.ui.components.EmptyState
import com.anlian.italase.shared.ui.components.GlassButton
import com.anlian.italase.shared.ui.components.GlassCard
import com.anlian.italase.shared.ui.components.PrimaryFab
import com.anlian.italase.shared.ui.components.StatusChip
import com.anlian.italase.shared.ui.components.enter
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
import italase.shared.generated.resources.action_save
import italase.shared.generated.resources.cd_add_debt
import italase.shared.generated.resources.debt_amount
import italase.shared.generated.resources.debt_empty_h
import italase.shared.generated.resources.debt_empty_t
import italase.shared.generated.resources.debt_form
import italase.shared.generated.resources.debt_from_cashier
import italase.shared.generated.resources.debt_name
import italase.shared.generated.resources.debt_note
import italase.shared.generated.resources.debt_pay_action
import italase.shared.generated.resources.debt_pay_amount
import italase.shared.generated.resources.debt_pay_title
import italase.shared.generated.resources.debt_progress
import italase.shared.generated.resources.debt_remaining
import italase.shared.generated.resources.debt_running
import italase.shared.generated.resources.debt_total
import italase.shared.generated.resources.status_paid
import italase.shared.generated.resources.tab_debt
import italase.shared.generated.resources.title_debts
import org.jetbrains.compose.resources.stringResource

/**
 * Layar daftar utang beserta kedua dialognya (catat manual, bayar).
 *
 * State diangkat penuh ke pemanggil — NavHost memegang `showAddDialog` dan `payId`.
 * `payId` non-null sekaligus menandai utang mana yang sedang dibayar; layar sengaja
 * tidak menyimpan id itu sendiri supaya sumber kebenarannya tetap satu.
 *
 * `onSavePay` hanya mengirim nominal: id-nya sudah dipegang pemanggil, dan
 * pembayaran tetap dijalankan walau `payId` keburu berubah.
 */
@Composable
fun DebtScreen(
    debts: List<DebtEntity>,
    message: String,
    showAddDialog: Boolean,
    payId: Long?,
    onAdd: () -> Unit = {},
    onPay: (Long) -> Unit = {},
    onDismissAdd: () -> Unit = {},
    onSaveDebt: (String, Long, String) -> Unit = { _, _, _ -> },
    onDismissPay: () -> Unit = {},
    onSavePay: (Long) -> Unit = {},
) {
    DebtContent(
        debts = debts,
        message = message,
        onAdd = onAdd,
        onPay = onPay,
    )

    if (showAddDialog) {
        DebtFormDialog(
            onDismiss = onDismissAdd,
            onSave = onSaveDebt,
        )
    }

    if (payId != null) {
        PayDebtDialog(
            onDismiss = onDismissPay,
            onSave = onSavePay,
        )
    }
}

/**
 * Bagian daftar utang tanpa dialog; dipakai layar penuh maupun preview.
 *
 * Observes: scroll behavior top bar (judul/eyebrow lewat [TopBarInfoEffect]) dan
 * `rememberAppear` untuk animasi masuk bertahap (kartu total, list, FAB).
 *
 * `debts` boleh memuat entri LUNAS — kartunya tetap tampil sebagai riwayat, hanya
 * tanpa tombol Bayar. Angka Total di kartu atas sengaja hanya menjumlah yang belum
 * lunas, jadi ia mengecil saat utang dibayar.
 */
@Composable
fun DebtContent(
    debts: List<DebtEntity>,
    message: String,
    onAdd: () -> Unit = {},
    onPay: (Long) -> Unit = {},
) {
    val active = debts.filter { it.status != DebtStatus.LUNAS }
    Box(Modifier.fillMaxSize()) {
        val scrollBehavior = LocalTopAppBarScrollBehavior.current
        val listState = rememberLazyListState()
        val appeared = rememberAppear()
        TopBarInfoEffect(
            eyebrow = stringResource(Res.string.debt_running, active.size),
            title = stringResource(Res.string.title_debts),
        )
        Column(
            Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
                .nestedScrollConnection(scrollBehavior, listState),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Spacer(Modifier.height(LocalTopOverlayPadding.current + 8.dp))
            // Kartu total: primitif kaca yang sama dengan layar lain. Warna
            // hijau tetap — yang berubah cuma bahannya (sheen, shadow, tanpa
            // BorderStroke). alpha 0.92: hampir pekat supaya angka tetap
            // terbaca, tapi blob latar masih samar menembus.
            GlassCard(
                modifier = Modifier.fillMaxWidth().enter(appeared, 0),
                tint = MaterialTheme.colorScheme.primary,
                alpha = 0.95f,
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text(
                        stringResource(Res.string.debt_total),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f),
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        formatRupiah(active.sumOf { it.remaining }),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimary,
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
            if (debts.isEmpty()) {
                EmptyState(
                    stringResource(Res.string.debt_empty_t),
                    stringResource(Res.string.debt_empty_h),
                )
            } else {
                LazyColumn(
                    state = listState,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f).enter(appeared, 1),
                    contentPadding = bottomActionTailPadding(ACTION_FAB_HEIGHT),
                ) {
                    items(debts, key = { it.id }) { d ->
                        val lunas = d.status == DebtStatus.LUNAS
                        val progress = if (d.totalAmount > 0) d.paidAmount.toFloat() / d.totalAmount.toFloat() else 1f
                        GlassCard(shape = RoundedCornerShape(20.dp)) {
                            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Row(
                                    Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Text(d.customerName, fontWeight = FontWeight.Bold)
                                    StatusChip(
                                        if (lunas) stringResource(Res.string.status_paid)
                                        else stringResource(Res.string.debt_remaining, formatRupiah(d.remaining)),
                                        good = lunas,
                                    )
                                }
                                LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth())
                                Text(
                                    stringResource(
                                        Res.string.debt_progress,
                                        formatRupiah(d.paidAmount),
                                        formatRupiah(d.totalAmount),
                                    ),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                val noteText = if (d.note.isNotBlank()) d.note
                                else stringResource(Res.string.debt_from_cashier, d.saleId ?: 0)
                                Text(
                                    noteText,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                if (!lunas) {
                                    GlassButton(
                                        text = stringResource(Res.string.debt_pay_action),
                                        onClick = { onPay(d.id) },
                                        height = 40.dp,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
        PrimaryFab(
            text = stringResource(Res.string.tab_debt),
            icon = AppIcon.ADD,
            contentDescription = stringResource(Res.string.cd_add_debt),
            onClick = onAdd,
            modifier = Modifier.align(Alignment.BottomEnd).aboveTabBar().enter(appeared, 2),
        )
    }
}

/**
 * Dialog catat utang baru dengan bahasa liquid glass.
 */
@Composable
fun DebtFormDialog(onDismiss: () -> Unit, onSave: (String, Long, String) -> Unit) {
    var name by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    val scheme = MaterialTheme.colorScheme

    GlassDialog(onDismiss = onDismiss) {
        Text(
            stringResource(Res.string.debt_form),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = scheme.onSurface,
        )
        Spacer(Modifier.height(14.dp))
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f, fill = false)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            GlassTextField(
                value = name,
                onValueChange = { name = it },
                label = stringResource(Res.string.debt_name),
            )
            GlassTextField(
                value = amount,
                onValueChange = { amount = it },
                label = stringResource(Res.string.debt_amount),
            )
            GlassTextField(
                value = note,
                onValueChange = { note = it },
                label = stringResource(Res.string.debt_note),
            )
        }
        Spacer(Modifier.height(18.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            GlassButton(
                text = stringResource(Res.string.action_cancel),
                onClick = onDismiss,
                container = scheme.onSurface.copy(alpha = 0.08f),
                content = scheme.onSurface,
                modifier = Modifier.weight(1f),
            )
            GlassButton(
                text = stringResource(Res.string.action_save),
                onClick = { onSave(name, parseRupiah(amount), note) },
                enabled = name.isNotBlank() && amount.isNotBlank(),
                modifier = Modifier.weight(1f),
            )
        }
    }
}

/**
 * Dialog pembayaran (cicilan) utang dengan bahasa liquid glass.
 */
@Composable
fun PayDebtDialog(onDismiss: () -> Unit, onSave: (Long) -> Unit) {
    var amount by remember { mutableStateOf("") }
    val scheme = MaterialTheme.colorScheme

    GlassDialog(onDismiss = onDismiss) {
        Text(
            stringResource(Res.string.debt_pay_title),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = scheme.onSurface,
        )
        Spacer(Modifier.height(14.dp))
        GlassTextField(
            value = amount,
            onValueChange = { amount = it },
            label = stringResource(Res.string.debt_pay_amount),
        )
        Spacer(Modifier.height(18.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            GlassButton(
                text = stringResource(Res.string.action_cancel),
                onClick = onDismiss,
                container = scheme.onSurface.copy(alpha = 0.08f),
                content = scheme.onSurface,
                modifier = Modifier.weight(1f),
            )
            GlassButton(
                text = stringResource(Res.string.action_save),
                onClick = { onSave(parseRupiah(amount)) },
                enabled = amount.isNotBlank(),
                modifier = Modifier.weight(1f),
            )
        }
    }
}

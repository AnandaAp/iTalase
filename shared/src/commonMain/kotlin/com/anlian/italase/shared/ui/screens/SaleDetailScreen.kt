package com.anlian.italase.shared.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import com.anlian.italase.shared.report.ReceiptFormatter
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
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
import com.anlian.italase.shared.ui.components.GlassShimmerCard
import com.anlian.italase.shared.ui.components.ShimmerCrossfade
import com.anlian.italase.shared.ui.components.GlassChip
import com.anlian.italase.shared.ui.components.glassSurface
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
import com.anlian.italase.shared.util.parseRupiah
import italase.shared.generated.resources.Res
import italase.shared.generated.resources.action_cancel
import italase.shared.generated.resources.action_edit
import italase.shared.generated.resources.action_save
import italase.shared.generated.resources.cash_m_cash
import italase.shared.generated.resources.cash_m_transfer
import italase.shared.generated.resources.cash_paid
import italase.shared.generated.resources.cash_change
import italase.shared.generated.resources.cash_short
import italase.shared.generated.resources.cash_short_hint
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
    onSaveEdit: (String, String, String, Long) -> Unit = { _, _, _, _ -> },
) {
    Box(Modifier.fillMaxSize()) {
        // FAB di sini milik composable ini; list-nya ada di SaleDetailContent
        // yang punya flag sendiri. Masing-masing scope komposisi, jadi tidak bisa
        // berbagi `remember`.
        val appeared = rememberAppear(detail != null)
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
            ShimmerCrossfade(
                loading = detail == null,
                modifier = Modifier.fillMaxWidth().weight(1f),
                shimmer = {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        GlassShimmerCard(height = 110.dp)
                        GlassShimmerCard(height = 160.dp)
                        GlassShimmerCard(height = 80.dp)
                    }
                },
                content = {
                    if (detail != null) {
                        SaleDetailContent(detail = detail, message = message)
                    }
                }
            )
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
            initialPayMethod = detail.sale.payMethod,
            initialTendered = detail.sale.tendered ?: detail.sale.paidAmount,
            total = detail.sale.total,
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
        item {
            var copiedReceiptType by remember { mutableStateOf<String?>(null) }
            val clipboard = LocalClipboardManager.current

            GlassCard(shape = RoundedCornerShape(20.dp)) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            AppIcon.RECEIPT.painter(),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp),
                        )
                        Text(
                            "Format Struk Transaksi",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleSmall,
                        )
                    }
                    Text(
                        "Salin teks struk untuk dicetak ke printer thermal Bluetooth (58mm) atau dibagikan ke WhatsApp.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        GlassButton(
                            text = "Thermal 58mm",
                            onClick = {
                                val text = ReceiptFormatter.formatThermal(detail)
                                clipboard.setText(AnnotatedString(text))
                                copiedReceiptType = "Thermal 58mm"
                            },
                            modifier = Modifier.weight(1f),
                            specular = 0.16f,
                        )
                        GlassButton(
                            text = "WhatsApp",
                            onClick = {
                                val text = ReceiptFormatter.formatWhatsApp(detail)
                                clipboard.setText(AnnotatedString(text))
                                copiedReceiptType = "WhatsApp"
                            },
                            modifier = Modifier.weight(1f),
                            specular = 0.16f,
                        )
                    }
                    if (copiedReceiptType != null) {
                        Text(
                            "Teks struk $copiedReceiptType berhasil disalin ke papan klip!",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold,
                        )
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
 * Dialog edit catatan dan nama pembeli nota dengan bahasa liquid glass.
 */
@Composable
fun SaleEditDialog(
    initialCustomer: String,
    initialNote: String,
    initialPayMethod: String = PayMethod.TUNAI,
    initialTendered: Long = 0L,
    total: Long = 0L,
    onDismiss: () -> Unit,
    onSave: (String, String, String, Long) -> Unit,
) {
    var customer by remember(initialCustomer) { mutableStateOf(initialCustomer) }
    var note by remember(initialNote) { mutableStateOf(initialNote) }
    var method by remember(initialPayMethod) { mutableStateOf(initialPayMethod) }
    var tenderedText by remember(initialTendered) {
        mutableStateOf(if (initialTendered > 0) initialTendered.toString() else "")
    }
    val tendered = parseRupiah(tenderedText)
    val scheme = MaterialTheme.colorScheme

    GlassFormDialog(
        title = stringResource(Res.string.detail_edit_title),
        onDismiss = onDismiss,
        onConfirm = { onSave(customer, note, method, tendered) },
    ) {
        if (total > 0) {
            Text(
                stringResource(Res.string.detail_total) + ": " + formatRupiah(total),
                style = MaterialTheme.typography.bodyMedium,
                color = scheme.primary,
                fontWeight = FontWeight.SemiBold,
            )
        }
        GlassTextField(
            value = customer,
            onValueChange = { customer = it },
            label = stringResource(Res.string.debt_name),
            keyboardOptions = KeyboardOptions(
                imeAction = ImeAction.Next,
            ),
        )

        GlassTextField(
            value = tenderedText,
            onValueChange = { input ->
                val digits = input.filter { it.isDigit() }
                if (digits.length <= 15) {
                    tenderedText = digits
                }
            },
            label = stringResource(Res.string.cash_paid),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Number,
                imeAction = ImeAction.Next,
            ),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
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
        }
        if (total > 0) {
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
                    if (tendered >= total) stringResource(Res.string.cash_change)
                    else stringResource(Res.string.cash_short),
                    color = scheme.onSurfaceVariant,
                )
                Text(
                    formatRupiah(if (tendered >= total) tendered - total else total - tendered),
                    fontWeight = FontWeight.Bold,
                    color = scheme.onSurface,
                )
            }
            if (method != PayMethod.BON && tendered < total) {
                Text(
                    stringResource(Res.string.cash_short_hint),
                    color = scheme.error,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
        GlassTextField(
            value = note,
            onValueChange = { note = it },
            label = stringResource(Res.string.debt_note),
            keyboardOptions = KeyboardOptions(
                imeAction = ImeAction.Done,
            ),
        )
    }
}

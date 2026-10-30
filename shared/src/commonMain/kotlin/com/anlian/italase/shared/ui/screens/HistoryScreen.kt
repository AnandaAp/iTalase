package com.anlian.italase.shared.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import com.anlian.italase.shared.data.local.entity.SaleEntity
import com.anlian.italase.shared.data.local.entity.TxnStatus
import com.anlian.italase.shared.ui.components.AppIcon
import com.anlian.italase.shared.ui.components.CategoryChips
import com.anlian.italase.shared.ui.components.DateRangeChip
import com.anlian.italase.shared.ui.components.DateRangePickerDialog
import com.anlian.italase.shared.ui.components.EmptyState
import com.anlian.italase.shared.ui.components.GlassEmptyState
import com.anlian.italase.shared.ui.components.GlassShimmerCard
import com.anlian.italase.shared.ui.components.ShimmerCrossfade
import com.anlian.italase.shared.ui.components.Enter
import com.anlian.italase.shared.ui.components.GlassCard
import com.anlian.italase.shared.ui.components.GlassIconButton
import com.anlian.italase.shared.ui.components.LoadMoreFooter
import com.anlian.italase.shared.ui.components.OnBottomReached
import com.anlian.italase.shared.ui.components.StatusChip
import com.anlian.italase.shared.ui.components.enter
import com.anlian.italase.shared.ui.components.rememberAppear
import com.anlian.italase.shared.ui.navigation.LocalBottomOverlayPadding
import com.anlian.italase.shared.ui.navigation.LocalTopAppBarScrollBehavior
import com.anlian.italase.shared.ui.navigation.LocalTopOverlayPadding
import com.anlian.italase.shared.ui.navigation.TopBarInfoEffect
import com.anlian.italase.shared.ui.navigation.nestedScrollConnection
import com.anlian.italase.shared.util.formatDateTimeLong
import com.anlian.italase.shared.util.formatRupiah
import italase.shared.generated.resources.Res
import italase.shared.generated.resources.cash_m_cash
import italase.shared.generated.resources.cash_m_transfer
import italase.shared.generated.resources.hist_count
import italase.shared.generated.resources.hist_empty_h
import italase.shared.generated.resources.hist_empty_t
import italase.shared.generated.resources.hist_export
import italase.shared.generated.resources.hist_profit
import italase.shared.generated.resources.hist_receipt
import italase.shared.generated.resources.status_paid
import italase.shared.generated.resources.tab_debt
import italase.shared.generated.resources.title_history
import kotlinx.datetime.LocalDate
import org.jetbrains.compose.resources.stringResource

/**
 * Layar riwayat penjualan dengan saringan kategori.
 *
 * `sales` hanya halaman yang sudah dimuat; [hasMore]/[loadingMore]/[onLoadMore]
 * adalah kontrak memuat-bertahap dari ViewModel, bukan paginasi halaman bernomor.
 * `activeCategory` null berarti semua kategori, dan filternya sengaja tetap dipegang
 * pemanggil supaya tidak hilang saat layar dibuka ulang. Menekan kartu memanggil
 * [onOpen] dengan id nota — pemanggil yang memuat detailnya.
 */
@Composable
fun HistoryScreen(
    sales: List<SaleEntity>,
    categories: List<String>,
    activeCategory: String?,
    loadingMore: Boolean = false,
    hasMore: Boolean = false,
    rangeFrom: LocalDate? = null,
    rangeTo: LocalDate? = null,
    exporting: Boolean = false,
    isLoading: Boolean = false,
    onCategory: (String?) -> Unit = {},
    onOpen: (Long) -> Unit = {},
    onLoadMore: () -> Unit = {},
    onRange: (LocalDate?, LocalDate?) -> Unit = { _, _ -> },
    onExport: () -> Unit = {},
) {
    HistoryContent(
        sales = sales,
        categories = categories,
        activeCategory = activeCategory,
        loadingMore = loadingMore,
        hasMore = hasMore,
        rangeFrom = rangeFrom,
        rangeTo = rangeTo,
        exporting = exporting,
        isLoading = isLoading,
        onCategory = onCategory,
        onOpen = onOpen,
        onLoadMore = onLoadMore,
        onRange = onRange,
        onExport = onExport,
    )
}

/**
 * Label metode bayar untuk satu baris riwayat.
 *
 * `@Composable` karena butuh `stringResource`. Nilai tak dikenal (mis. data lama
 * atau metode baru yang belum punya label) jatuh ke label utang, sama seperti
 * pemetaan di layar detail.
 */
@Composable
private fun payMethodLabel(method: String): String = when (method) {
    PayMethod.TUNAI -> stringResource(Res.string.cash_m_cash)
    PayMethod.TRANSFER -> stringResource(Res.string.cash_m_transfer)
    PayMethod.QRIS -> PayMethod.QRIS
    else -> stringResource(Res.string.tab_debt)
}

/**
 * Bagian daftar riwayat tanpa pembungkus; dipakai layar penuh maupun preview.
 *
 * Observes: scroll behavior top bar (judul/eyebrow), `rememberAppear` untuk animasi
 * masuk, dan `listState` untuk pemicu muat-tambahan.
 *
 * Saringan kategori tetap dirender walau `sales` kosong — hasil filter yang nol
 * harus tetap bisa dilepas kembali, bukan terkunci di layar kosong.
 */
@Composable
fun HistoryContent(
    sales: List<SaleEntity>,
    categories: List<String> = emptyList(),
    activeCategory: String? = null,
    loadingMore: Boolean = false,
    hasMore: Boolean = false,
    rangeFrom: LocalDate? = null,
    rangeTo: LocalDate? = null,
    exporting: Boolean = false,
    isLoading: Boolean = false,
    onCategory: (String?) -> Unit = {},
    onOpen: (Long) -> Unit = {},
    onLoadMore: () -> Unit = {},
    onRange: (LocalDate?, LocalDate?) -> Unit = { _, _ -> },
    onExport: () -> Unit = {},
) {
    val scrollBehavior = LocalTopAppBarScrollBehavior.current
    val listState = rememberLazyListState()
    val appeared = rememberAppear(!isLoading)
    var pickingRange by remember { mutableStateOf(false) }
    TopBarInfoEffect(
        eyebrow = stringResource(Res.string.hist_count, sales.size),
        title = stringResource(Res.string.title_history),
    )
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val isLandscape = (maxWidth > maxHeight && maxWidth >= 540.dp) || maxWidth >= 600.dp

        Column(
            Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
                .nestedScrollConnection(scrollBehavior, listState),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Spacer(Modifier.height(LocalTopOverlayPadding.current + 8.dp))
            Enter(appeared, 0) {
                if (isLandscape) {
                    // Tata letak Adaptive Landscape: Satu baris terpadu, seimbang, dan proporsional.
                    // Kiri: Saringan Kategori (fleksibel scroll horizontal),
                    // Kanan: Tombol Filter Rentang Tanggal + Tombol Cetak / Ekspor berdampingan.
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        if (categories.isNotEmpty()) {
                            Box(modifier = Modifier.weight(1f)) {
                                CategoryChips(
                                    categories = categories,
                                    active = activeCategory,
                                    onSelect = onCategory,
                                )
                            }
                            Spacer(Modifier.width(12.dp))
                        }
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            DateRangeChip(
                                from = rangeFrom,
                                to = rangeTo,
                                onClick = { pickingRange = true },
                            )
                            GlassIconButton(
                                icon = AppIcon.RECEIPT,
                                contentDescription = stringResource(Res.string.hist_export),
                                onClick = onExport,
                                enabled = !exporting && sales.isNotEmpty(),
                            )
                        }
                    }
                } else {
                    // Tata letak Portrait Standar
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        CategoryChips(
                            categories = categories,
                            active = activeCategory,
                            onSelect = onCategory,
                        )
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            DateRangeChip(
                                from = rangeFrom,
                                to = rangeTo,
                                onClick = { pickingRange = true },
                            )
                            Spacer(Modifier.weight(1f))
                            GlassIconButton(
                                icon = AppIcon.RECEIPT,
                                contentDescription = stringResource(Res.string.hist_export),
                                onClick = onExport,
                                enabled = !exporting && sales.isNotEmpty(),
                            )
                        }
                    }
                }
            }
        ShimmerCrossfade(
            loading = isLoading,
            modifier = Modifier.weight(1f),
            shimmer = {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    repeat(5) {
                        GlassShimmerCard(height = 92.dp, shape = RoundedCornerShape(20.dp))
                    }
                }
            },
            content = {
                if (sales.isEmpty()) {
                    GlassEmptyState(
                        stringResource(Res.string.hist_empty_t),
                        stringResource(Res.string.hist_empty_h),
                    )
                } else {
                    // Pemicu muat-tambahan dipasang DI LUAR LazyColumn
                    listState.OnBottomReached(
                        enabled = hasMore && !loadingMore,
                        onLoadMore = onLoadMore,
                    )
                    LazyColumn(
                        state = listState,
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxSize(),
                        // Ruang ekor: baris terakhir bisa digulir ke atas tab bar.
                        contentPadding = PaddingValues(bottom = LocalBottomOverlayPadding.current),
                    ) {
                        items(sales, key = { it.id }) { s ->
                    val lunas = s.status == TxnStatus.LUNAS
                    GlassCard(shape = RoundedCornerShape(20.dp), onClick = { onOpen(s.id) }) {
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
                                    // Tanggal transaksi: dulu tidak ditampilkan sama
                                    // sekali, sehingga "Struk #3" tidak bisa dipetakan
                                    // ke hari kejadian. Format panjang dipakai karena
                                    // inilah yang dibaca user saat mencocokkan bon
                                    // fisik dengan catatan.
                                    Text(
                                        formatDateTimeLong(s.dateMillis),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
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
                    item(key = "load-more") { LoadMoreFooter(loadingMore) }
                }
            }
        },
    )
}
}

    if (pickingRange) {
        DateRangePickerDialog(
            from = rangeFrom,
            to = rangeTo,
            onApply = { f, t2 ->
                pickingRange = false
                onRange(f, t2)
            },
            onDismiss = { pickingRange = false },
        )
    }
}

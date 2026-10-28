package com.anlian.italase.shared.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
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
import com.anlian.italase.shared.util.formatDateLong
import com.anlian.italase.shared.util.formatDateRange
import com.anlian.italase.shared.util.localDateOf
import com.anlian.italase.shared.util.minusDays
import com.anlian.italase.shared.util.nowMillis
import com.anlian.italase.shared.util.startOfDayMillis
import italase.shared.generated.resources.Res
import italase.shared.generated.resources.action_cancel
import italase.shared.generated.resources.range_all
import italase.shared.generated.resources.range_apply
import italase.shared.generated.resources.range_from
import italase.shared.generated.resources.range_pick
import italase.shared.generated.resources.range_to
import kotlinx.datetime.LocalDate
import org.jetbrains.compose.resources.stringResource

/** Preset rentang yang paling sering dipakai warung. */
private val RANGE_PRESETS = listOf(7, 30, 90)

/**
 * Tombol pembuka pemilih rentang + pintasan.
 *
 * Chip ini menampilkan rentang AKTIF dalam bahasa Indonesia, jadi user bisa
 * membaca filternya tanpa membuka apa pun. Pemilihnya sengaja berupa daftar
 * tanggal, bukan `DatePickerDialog` Material: dialog sistem tidak ada di
 * `commonMain`, dan menulisakan `expect`/`actual` pemilih tanggal untuk satu
 * layar jauh lebih mahal daripada daftar yang bisa digulir.
 */
@Composable
fun DateRangeChip(
    from: LocalDate?,
    to: LocalDate?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val label = formatDateRange(from, to)
    GlassChip(
        text = label,
        selected = from != null || to != null,
        onClick = onClick,
        modifier = modifier
    )
}

/**
 * Dialog pemilih rentang: dua tombol tanggal + pintasan 7/30/90 hari.
 *
 * `onApply(null, null)` berarti "seluruh riwayat" dan sengaja tersedia sebagai
 * aksi eksplisit — user yang sudah memilih rentang harus bisa melepasnya kembali
 * tanpa menebak. Adaptif terhadap mode landscape dan layar tablet.
 */
@Composable
fun DateRangePickerDialog(
    from: LocalDate?,
    to: LocalDate?,
    onApply: (LocalDate?, LocalDate?) -> Unit,
    onDismiss: () -> Unit,
) {
    var start by remember { mutableStateOf(from) }
    var end by remember { mutableStateOf(to) }
    var picking by remember { mutableStateOf<Field?>(null) }

    GlassDialog(onDismiss = onDismiss) {
        val isLandscape = LocalIsLandscape.current
        val today = remember { localDateOf(nowMillis()) }

        if (isLandscape) {
            // Adaptive Landscape: Header sejajar dengan tombol aksi cepat
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        stringResource(Res.string.range_pick),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        formatDateRange(start, end),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    GlassButton(
                        text = stringResource(Res.string.action_cancel),
                        onClick = onDismiss,
                        container = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f),
                        content = MaterialTheme.colorScheme.onSurface,
                        elevation = 0.dp,
                        specular = 0.18f,
                    )
                    GlassButton(
                        text = stringResource(Res.string.range_apply),
                        onClick = { onApply(start, end) },
                    )
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 240.dp)
                    .verticalScroll(rememberScrollState())
                    .padding(top = 10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    RangeField(
                        label = stringResource(Res.string.range_from),
                        value = start?.let { formatDateLong(startOfDayMillis(it)!!) },
                        modifier = Modifier.weight(1f),
                    ) { picking = Field.START }
                    RangeField(
                        label = stringResource(Res.string.range_to),
                        value = end?.let { formatDateLong(startOfDayMillis(it)!!) },
                        modifier = Modifier.weight(1f),
                    ) { picking = Field.END }
                }

                FlowRow(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    GlassChip(
                        text = stringResource(Res.string.range_all),
                        selected = start == null && end == null,
                        onClick = {
                            start = null
                            end = null
                        },
                    )
                    RANGE_PRESETS.forEach { days ->
                        GlassChip(
                            text = "$days hari terakhir",
                            selected = start == today.minusDays(days - 1) && end == today,
                            onClick = {
                                start = today.minusDays(days - 1)
                                end = today
                            },
                        )
                    }
                }
            }
        } else {
            // Portrait Standar
            Text(
                stringResource(Res.string.range_pick),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
            Text(
                formatDateRange(start, end),
                modifier = Modifier.padding(top = 4.dp),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Row(
                Modifier.fillMaxWidth().padding(top = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                RangeField(
                    label = stringResource(Res.string.range_from),
                    value = start?.let { formatDateLong(startOfDayMillis(it)!!) },
                    modifier = Modifier.weight(1f),
                ) { picking = Field.START }
                RangeField(
                    label = stringResource(Res.string.range_to),
                    value = end?.let { formatDateLong(startOfDayMillis(it)!!) },
                    modifier = Modifier.weight(1f),
                ) { picking = Field.END }
            }

            FlowRow(
                Modifier.fillMaxWidth().padding(top = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                GlassChip(
                    text = stringResource(Res.string.range_all),
                    selected = start == null && end == null,
                    onClick = {
                        start = null
                        end = null
                    },
                )
                RANGE_PRESETS.forEach { days ->
                    GlassChip(
                        text = "$days hari terakhir",
                        selected = start == today.minusDays(days - 1) && end == today,
                        onClick = {
                            start = today.minusDays(days - 1)
                            end = today
                        },
                    )
                }
            }

            Row(
                Modifier.fillMaxWidth().padding(top = 18.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                GlassButton(
                    text = stringResource(Res.string.action_cancel),
                    onClick = onDismiss,
                    container = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f),
                    content = MaterialTheme.colorScheme.onSurface,
                    elevation = 0.dp,
                    specular = 0.18f,
                    modifier = Modifier.weight(1f),
                )
                GlassButton(
                    text = stringResource(Res.string.range_apply),
                    onClick = { onApply(start, end) },
                    modifier = Modifier.weight(1f),
                )
            }
        }

        if (picking != null) {
            DateListPicker(
                initial = if (picking == Field.START) start else end,
                onPick = { picked ->
                    if (picking == Field.START) start = picked else end = picked
                    picking = null
                },
                onDismiss = { picking = null },
            )
        }
    }
}

private enum class Field { START, END }

/** Satu tombol teks berlabel — dipakai untuk sisi "dari" dan "sampai". */
@Composable
private fun RangeField(
    label: String,
    value: String?,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    GlassCard(
        shape = RoundedCornerShape(16.dp),
        onClick = onClick,
        modifier = modifier
    ) {
        Column(Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
            Text(
                label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Bold,
            )
            Text(
                value ?: "—",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

/**
 * Pemilih tanggal berupa daftar, 120 hari terakhir.
 *
 * Rentang 120 hari dipilih karena riwayat warung yang relevan hampir selalu di
 * dalam itu; daftar tak terbatas berarti menggulir ratusan baris untuk data
 * tahun lalu. Rentang yang lebih tua tetap bisa dicapai setelah filter ini
 * diubah menjadi pemilih bulan pada iterasi berikutnya.
 *
 * Memiliki batas tinggi maksimum adaptif (`heightIn`) agar aman diorientasikan
 * secara vertikal maupun horizontal (landscape) tanpa mengalami overflow.
 */
@Composable
private fun DateListPicker(
    initial: LocalDate?,
    onPick: (LocalDate) -> Unit,
    onDismiss: () -> Unit,
) {
    val isLandscape = LocalIsLandscape.current
    val today: LocalDate = remember { localDateOf(nowMillis()) }
    val dates: List<LocalDate> = remember(today) { (0 until 120).map { today.minusDays(it) } }
    GlassDialog(onDismiss = onDismiss) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                stringResource(Res.string.range_pick),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            GlassButton(
                text = stringResource(Res.string.action_cancel),
                onClick = onDismiss,
                container = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f),
                content = MaterialTheme.colorScheme.onSurface,
                elevation = 0.dp,
                specular = 0.18f,
            )
        }
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = if (isLandscape) 200.dp else 360.dp)
                .padding(top = 8.dp),
        ) {
            items(dates) { date ->
                val selected = date == initial
                Row(
                    Modifier.fillMaxWidth().padding(vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    GlassChip(
                        text = formatDateLong(startOfDayMillis(date)!!),
                        selected = selected,
                        onClick = { onPick(date) },
                    )
                    if (selected) {
                        Icon(
                            AppIcon.CHEVRON_RIGHT.painter(),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp),
                        )
                    }
                }
            }
        }
    }
}

package com.anlian.italase.shared.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.anlian.italase.shared.ui.components.AppIcon
import com.anlian.italase.shared.data.local.entity.QrisConfigEntity
import com.anlian.italase.shared.ui.components.GlassButton
import com.anlian.italase.shared.ui.components.GlassCard
import com.anlian.italase.shared.ui.components.GlassConfirmDialog
import com.anlian.italase.shared.ui.components.StatusChip
import com.anlian.italase.shared.ui.components.enter
import com.anlian.italase.shared.ui.components.painter
import com.anlian.italase.shared.ui.components.rememberAppear
import com.anlian.italase.shared.ui.navigation.LocalBottomOverlayPadding
import com.anlian.italase.shared.ui.navigation.LocalTopAppBarScrollBehavior
import com.anlian.italase.shared.ui.navigation.LocalTopOverlayPadding
import com.anlian.italase.shared.ui.navigation.TopBarInfoEffect
import com.anlian.italase.shared.ui.navigation.nestedScrollConnection
import com.anlian.italase.shared.util.formatDateTime
import org.jetbrains.compose.resources.decodeToImageBitmap

/**
 * Layar Pengaturan Metode Pembayaran & Konfigurasi QRIS Toko (AUD-107).
 *
 * Mendukung tata letak adaptif (Portrait & Landscape split-pane) dan sinkronisasi
 * scroll top bar liquid glass (docking title, specular rim, elevation shadow).
 */
@Composable
fun PaymentSettingsScreen(
    qrisConfig: QrisConfigEntity?,
    onToggleEnabled: (Boolean) -> Unit = {},
    onNavigateSetup: () -> Unit = {},
    onDeleteQris: () -> Unit = {},
    message: String? = null,
    onClearMessage: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val appeared = rememberAppear()
    val scrollBehavior = LocalTopAppBarScrollBehavior.current
    val scrollState = rememberScrollState()

    var showDeleteConfirm by remember { mutableStateOf(false) }

    LaunchedEffect(scrollState.value) {
        scrollBehavior?.state?.contentOffset = -scrollState.value.toFloat()
    }

    TopBarInfoEffect(
        eyebrow = "PENGATURAN",
        title = "Metode Pembayaran",
    )

    val isConfigured = qrisConfig?.imageBytes != null
    val isEnabled = isConfigured && qrisConfig.isEnabled

    BoxWithConstraints(modifier.fillMaxSize()) {
        val isLandscape = maxWidth > maxHeight
        val topSpacerHeight = LocalTopOverlayPadding.current + (if (isLandscape) 6.dp else 14.dp)
        val bottomSpacerHeight = LocalBottomOverlayPadding.current + (if (isLandscape) 14.dp else 24.dp)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .nestedScrollConnection(scrollBehavior, null)
                .verticalScroll(scrollState)
                .padding(horizontal = 16.dp),
        ) {
            Spacer(Modifier.height(topSpacerHeight))

            if (isLandscape) {
                // Adaptive Split-Pane untuk Mode Landscape & Tablet
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    // Kolom Kiri: Status Switch + Preview Barcode / Setup
                    Column(
                        modifier = Modifier.weight(1.15f),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        QrisStatusToggleCard(
                            isConfigured = isConfigured,
                            isEnabled = isEnabled,
                            onToggle = onToggleEnabled,
                            modifier = Modifier.enter(appeared, 0),
                        )
                        QrisBarcodeCard(
                            qrisConfig = qrisConfig,
                            onNavigateSetup = onNavigateSetup,
                            onRequestDelete = { showDeleteConfirm = true },
                            modifier = Modifier.enter(appeared, 1),
                        )
                    }

                    // Kolom Kanan: Panduan & Edukasi Keamanan
                    Column(
                        modifier = Modifier.weight(0.85f),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        QrisEducationCard(
                            modifier = Modifier.enter(appeared, 2),
                        )
                    }
                }
            } else {
                // Layout Vertikal untuk Mode Portrait
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    QrisStatusToggleCard(
                        isConfigured = isConfigured,
                        isEnabled = isEnabled,
                        onToggle = onToggleEnabled,
                        modifier = Modifier.enter(appeared, 0),
                    )
                    QrisBarcodeCard(
                        qrisConfig = qrisConfig,
                        onNavigateSetup = onNavigateSetup,
                        onRequestDelete = { showDeleteConfirm = true },
                        modifier = Modifier.enter(appeared, 1),
                    )
                    QrisEducationCard(
                        modifier = Modifier.enter(appeared, 2),
                    )
                }
            }

            Spacer(Modifier.height(bottomSpacerHeight))
        }
    }

    // Dialog Konfirmasi Hapus QRIS
    if (showDeleteConfirm) {
        GlassConfirmDialog(
            title = "Hapus Konfigurasi QRIS?",
            message = "Foto barcode QRIS akan dihapus dari aplikasi. Metode pembayaran QRIS tidak akan muncul lagi di kasir sampai Anda mengunggahnya kembali.",
            confirmText = "Hapus",
            onConfirm = {
                onDeleteQris()
                showDeleteConfirm = false
            },
            onDismiss = { showDeleteConfirm = false },
            eyebrow = "KONFIRMASI",
            cancelText = "Batal",
        )
    }
}

/**
 * Kartu Toggle Switch Status QRIS Kasir.
 */
@Composable
private fun QrisStatusToggleCard(
    isConfigured: Boolean,
    isEnabled: Boolean,
    onToggle: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val scheme = MaterialTheme.colorScheme
    GlassCard(modifier = modifier) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 16.dp),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        text = "QRIS Kasir",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = scheme.onSurface,
                    )
                    if (isConfigured) {
                        StatusChip(
                            text = if (isEnabled) "Aktif" else "Nonaktif",
                            good = isEnabled,
                        )
                    } else {
                        StatusChip(text = "Belum Diatur", good = false)
                    }
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    text = if (isConfigured) {
                        "Tampilkan metode QRIS dan barcode saat kasir memproses transaksi pembayaran."
                    } else {
                        "Unggah barcode QRIS toko Anda agar pelanggan dapat membayar secara nontunai."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = scheme.onSurfaceVariant,
                )
            }

            Switch(
                checked = isEnabled,
                onCheckedChange = { onToggle(it) },
                enabled = isConfigured,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = scheme.onPrimary,
                    checkedTrackColor = scheme.primary,
                    uncheckedThumbColor = scheme.outline,
                    uncheckedTrackColor = scheme.surfaceContainerHighest,
                ),
            )
        }
    }
}

/**
 * Kartu Penampil Barcode QRIS Toko atau Tombol Setup bila belum diatur.
 */
@Composable
private fun QrisBarcodeCard(
    qrisConfig: QrisConfigEntity?,
    onNavigateSetup: () -> Unit,
    onRequestDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scheme = MaterialTheme.colorScheme
    val imageBytes = qrisConfig?.imageBytes

    if (imageBytes != null) {
        GlassCard(modifier = modifier) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(3.dp),
                ) {
                    Text(
                        text = "Barcode QRIS Toko",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = scheme.onSurface,
                    )
                    if (qrisConfig.updatedAt > 0L) {
                        Text(
                            text = "Diperbarui: ${formatDateTime(qrisConfig.updatedAt)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = scheme.onSurfaceVariant,
                        )
                    }
                }

                // Barcode Viewer Surface: Background Putih Murni untuk Scanner Pelanggan
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.White)
                        .padding(14.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    val bitmap: ImageBitmap? = remember(imageBytes) {
                        try {
                            imageBytes.decodeToImageBitmap()
                        } catch (_: Throwable) {
                            null
                        }
                    }

                    if (bitmap != null) {
                        Image(
                            bitmap = bitmap,
                            contentDescription = "Barcode QRIS Warung",
                            // Ukuran mengikuti tinggi (maks 420.dp), lebar menyesuaikan rasio
                            // aspek bitmap: foto QRIS potret tampil utuh & besar di tengah
                            // panggung putih, bukan menyusut jadi strip sempit seperti
                            // sebelumnya (fillMaxWidth + maks 190.dp). Foto lanskap tetap
                            // dibatasi lebar kontainer oleh ContentScale.Fit.
                            modifier = Modifier
                                .heightIn(max = 420.dp),
                            contentScale = ContentScale.Fit,
                        )
                    } else {
                        Text(
                            text = "Gagal memuat gambar barcode",
                            color = Color.Gray,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }

                // Metadata File Citra
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    val sizeKb = imageBytes.size / 1024
                    Text(
                        text = "$sizeKb KB • JPEG (Optimal)",
                        style = MaterialTheme.typography.labelSmall,
                        color = scheme.onSurfaceVariant,
                    )
                    StatusChip(
                        text = "Siap Scan",
                        good = true,
                    )
                }
                Spacer(Modifier.height(4.dp))

                // Tombol Aksi: Ganti Foto & Hapus
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    GlassButton(
                        text = "Ganti Foto",
                        onClick = onNavigateSetup,
                        container = scheme.primary.copy(alpha = 0.14f),
                        content = scheme.primary,
                        height = 42.dp,
                        modifier = Modifier.weight(1f),
                    )
                    GlassButton(
                        text = "Hapus QRIS",
                        onClick = onRequestDelete,
                        container = scheme.error.copy(alpha = 0.14f),
                        content = scheme.error,
                        height = 42.dp,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    } else {
        // Empty State Card
        GlassCard(modifier = modifier) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Icon(
                    painter = AppIcon.PAYMENTS.painter(),
                    contentDescription = null,
                    tint = scheme.primary,
                    modifier = Modifier.size(54.dp),
                )
                Text(
                    text = "QRIS Belum Dikonfigurasi",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = scheme.onSurface,
                )
                Text(
                    text = "Toko Anda belum memiliki barcode QRIS. Tambahkan foto atau barcode QRIS warung Anda agar pelanggan dapat membayar langsung via bank atau e-wallet (BCA, Mandiri, GoPay, OVO, ShopeePay, dll).",
                    style = MaterialTheme.typography.bodySmall,
                    color = scheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 8.dp),
                )
                Spacer(Modifier.height(8.dp))
                GlassButton(
                    text = "Setup QRIS Sekarang",
                    onClick = onNavigateSetup,
                    container = scheme.primary,
                    content = scheme.onPrimary,
                    height = 46.dp,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

/**
 * Kartu Informasi Edukasi & Prosedur Transaksi QRIS Warung.
 */
@Composable
private fun QrisEducationCard(modifier: Modifier = Modifier) {
    val scheme = MaterialTheme.colorScheme
    GlassCard(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    painter = AppIcon.STOREFRONT.painter(),
                    contentDescription = null,
                    tint = scheme.primary,
                    modifier = Modifier
                        .size(20.dp)
                        .padding(end = 8.dp),
                )
                Text(
                    text = "Petunjuk Pembayaran QRIS Warung",
                    fontWeight = FontWeight.SemiBold,
                    style = MaterialTheme.typography.bodyMedium,
                    color = scheme.onSurface,
                )
            }
            Text(
                text = "• Barcode otomatis dikompresi (< 150 KB) tanpa mengurangi keterbacaan scan kamera pembeli.\n" +
                    "• Konfirmasi keberhasilan pembayaran tetap dilakukan manual oleh kasir demi keamanan warung.\n" +
                    "• Transaksi QRIS akan tercatat otomatis dengan status Lunas pada laporan penjualan.",
                style = MaterialTheme.typography.bodySmall,
                color = scheme.onSurfaceVariant,
            )
        }
    }
}

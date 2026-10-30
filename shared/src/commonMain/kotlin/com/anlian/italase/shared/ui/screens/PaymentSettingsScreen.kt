package com.anlian.italase.shared.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import com.anlian.italase.shared.data.local.entity.QrisConfigEntity
import com.anlian.italase.shared.ui.components.AppIcon
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
 * Layar Pengaturan Metode Pembayaran: Konfigurasi QRIS kasir (AUD-107).
 *
 * Mengikuti desain Warung Liquid Glass: observasi reaktif terhadap [qrisConfig],
 * switch toggle aktivasi, preview barcode resolusi tinggi pada container putih kontras tinggi,
 * metadata file & waktu pembaruan, aksi ganti/hapus dengan konfirmasi, dan empty state.
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
    val scheme = MaterialTheme.colorScheme

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

    Column(
        modifier = modifier
            .fillMaxSize()
            .enter(appeared, 0)
            .nestedScrollConnection(scrollBehavior, null)
            .verticalScroll(scrollState)
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Spacer(Modifier.height(LocalTopOverlayPadding.current + 8.dp))

        // 1. Kartu Toggle Switch Status QRIS
        GlassCard {
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
                    onCheckedChange = { onToggleEnabled(it) },
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

        // 2. Pratinjau Barcode atau Empty State
        val imageBytes = qrisConfig?.imageBytes
        if (imageBytes != null) {
            GlassCard {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
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
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 180.dp, max = 260.dp),
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
                            text = "Ukuran file: $sizeKb KB • JPEG (Optimal)",
                            style = MaterialTheme.typography.bodySmall,
                            color = scheme.onSurfaceVariant,
                        )
                        Text(
                            text = "Siap scan pembeli",
                            style = MaterialTheme.typography.labelSmall,
                            color = scheme.primary,
                            fontWeight = FontWeight.Medium,
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
                            onClick = { showDeleteConfirm = true },
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
            GlassCard {
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

        // 3. Kartu Informasi Edukasi / Petunjuk
        GlassCard {
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

        Spacer(Modifier.height(LocalBottomOverlayPadding.current + 32.dp))
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

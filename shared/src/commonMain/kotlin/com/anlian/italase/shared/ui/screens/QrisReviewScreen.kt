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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.anlian.italase.shared.ui.components.GlassButton
import com.anlian.italase.shared.ui.components.GlassCard
import com.anlian.italase.shared.ui.components.StatusChip
import com.anlian.italase.shared.ui.components.enter
import com.anlian.italase.shared.ui.components.rememberAppear
import com.anlian.italase.shared.ui.navigation.LocalBottomOverlayPadding
import com.anlian.italase.shared.ui.navigation.LocalTopAppBarScrollBehavior
import com.anlian.italase.shared.ui.navigation.LocalTopOverlayPadding
import com.anlian.italase.shared.ui.navigation.nestedScrollConnection
import com.anlian.italase.shared.ui.navigation.TopBarInfoEffect
import com.anlian.italase.shared.ui.theme.WarungGreen
import org.jetbrains.compose.resources.decodeToImageBitmap

/**
 * Layar Review & Konfirmasi Barcode QRIS (AUD-107).
 *
 * Menampilkan pratinjau citra yang baru diambil via [decodeToImageBitmap],
 * menyediakan tombol Ambil Ulang dan Simpan & Aktifkan.
 */
@Composable
fun QrisReviewScreen(
    rawBytes: ByteArray?,
    isSaving: Boolean,
    errorMessage: String?,
    onRetake: () -> Unit,
    onSaveAndActivate: () -> Unit,
    onClearError: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    TopBarInfoEffect(
        eyebrow = "KONFIRMASI",
        title = "Pratinjau QRIS",
    )

    // Jika rawBytes null (misalnya akibat memory eviction), kembali secara aman
    LaunchedEffect(rawBytes) {
        if (rawBytes == null || rawBytes.isEmpty()) {
            onRetake()
        }
    }

    val imageBitmap: ImageBitmap? = remember(rawBytes) {
        rawBytes?.let { bytes ->
            try {
                bytes.decodeToImageBitmap()
            } catch (_: Throwable) {
                null
            }
        }
    }

    QrisReviewContent(
        imageBitmap = imageBitmap,
        isSaving = isSaving,
        errorMessage = errorMessage,
        onRetake = onRetake,
        onSaveAndActivate = onSaveAndActivate,
        modifier = modifier,
    )
}

/**
 * Konten visual murni Review QRIS untuk Compose @Preview dan responsive split-pane.
 */
@Composable
fun QrisReviewContent(
    imageBitmap: ImageBitmap?,
    isSaving: Boolean,
    errorMessage: String?,
    onRetake: () -> Unit,
    onSaveAndActivate: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val appeared = rememberAppear()
    val scrollBehavior = LocalTopAppBarScrollBehavior.current
    val scrollState = rememberScrollState()

    LaunchedEffect(scrollState.value) {
        scrollBehavior?.state?.contentOffset = -scrollState.value.toFloat()
    }

    BoxWithConstraints(modifier.fillMaxSize()) {
        val isLandscape = maxWidth > maxHeight

        Column(
            Modifier
                .fillMaxSize()
                .nestedScrollConnection(scrollBehavior, null)
                .verticalScroll(scrollState)
                .padding(horizontal = 16.dp),
        ) {
            Spacer(Modifier.height(LocalTopOverlayPadding.current + (if (isLandscape) 6.dp else 14.dp)))

            if (isLandscape) {
                // Layout 2 Kolom untuk Lanskap (Split-Pane)
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    // Preview gambar di kiri
                    Box(
                        Modifier
                            .weight(1.1f)
                            .enter(appeared, 0),
                        contentAlignment = Alignment.Center,
                    ) {
                        ImagePreviewCard(imageBitmap = imageBitmap, maxHeight = 300)
                    }

                    // Keterangan dan aksi di kanan
                    Column(
                        Modifier
                            .weight(0.9f)
                            .enter(appeared, 1),
                    ) {
                        ReviewInfoBlock(errorMessage = errorMessage)
                        Spacer(Modifier.height(20.dp))
                        ActionButtonsBlock(
                            isSaving = isSaving,
                            hasValidImage = imageBitmap != null,
                            onRetake = onRetake,
                            onSaveAndActivate = onSaveAndActivate,
                        )
                    }
                }
            } else {
                // Layout Vertikal untuk Potret
                Column(
                    Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .enter(appeared, 0),
                        contentAlignment = Alignment.Center,
                    ) {
                        ImagePreviewCard(imageBitmap = imageBitmap, maxHeight = 340)
                    }

                    Spacer(Modifier.height(16.dp))

                    ReviewInfoBlock(
                        errorMessage = errorMessage,
                        modifier = Modifier.enter(appeared, 1),
                    )

                    Spacer(Modifier.height(24.dp))

                    ActionButtonsBlock(
                        isSaving = isSaving,
                        hasValidImage = imageBitmap != null,
                        onRetake = onRetake,
                        onSaveAndActivate = onSaveAndActivate,
                        modifier = Modifier.enter(appeared, 2),
                    )
                }
            }

            Spacer(Modifier.height(LocalBottomOverlayPadding.current + (if (isLandscape) 14.dp else 24.dp)))
        }
    }
}

@Composable
private fun ImagePreviewCard(
    imageBitmap: ImageBitmap?,
    maxHeight: Int,
    modifier: Modifier = Modifier,
) {
    GlassCard(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 220.dp, max = maxHeight.dp),
        shape = RoundedCornerShape(20.dp),
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .padding(14.dp),
            contentAlignment = Alignment.Center,
        ) {
            if (imageBitmap != null) {
                // Container putih kontras tinggi untuk preview kode QR
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.White)
                        .padding(8.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Image(
                        bitmap = imageBitmap,
                        contentDescription = "Pratinjau Barcode QRIS",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            } else {
                Text(
                    text = "Pratinjau tidak dapat dimuat",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun ReviewInfoBlock(
    errorMessage: String?,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = "Barcode Terpilih",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            StatusChip(text = "Siap Dioptimasi", good = true)
        }
        Spacer(Modifier.height(6.dp))
        Text(
            text = "Pastikan kode QR terlihat tajam, tidak silau, dan seluruh batas sudut barcode berada di dalam bidang gambar agar mudah dipindai pembeli.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            lineHeight = 18.sp,
        )

        if (!errorMessage.isNullOrBlank()) {
            Spacer(Modifier.height(10.dp))
            Box(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.85f))
                    .padding(horizontal = 12.dp, vertical = 8.dp),
            ) {
                Text(
                    text = errorMessage,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
    }
}

@Composable
private fun ActionButtonsBlock(
    isSaving: Boolean,
    hasValidImage: Boolean,
    onRetake: () -> Unit,
    onSaveAndActivate: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scheme = MaterialTheme.colorScheme

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        GlassButton(
            text = "Ambil Ulang",
            onClick = onRetake,
            enabled = !isSaving,
            container = scheme.surfaceContainerHigh,
            content = scheme.onSurface,
            elevation = 2.dp,
            specular = 0.15f,
            modifier = Modifier.weight(1f),
        )

        GlassButton(
            text = if (isSaving) "Menyimpan…" else "Simpan & Aktifkan",
            onClick = onSaveAndActivate,
            enabled = !isSaving && hasValidImage,
            container = WarungGreen,
            content = Color.White,
            elevation = 10.dp,
            specular = 0.32f,
            modifier = Modifier.weight(1.4f),
        )
    }
}

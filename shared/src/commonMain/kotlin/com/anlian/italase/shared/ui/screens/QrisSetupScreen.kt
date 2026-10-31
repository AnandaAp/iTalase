package com.anlian.italase.shared.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.anlian.italase.shared.ui.components.AnnounceGlassDialogOpen
import com.anlian.italase.shared.ui.components.AppIcon
import com.anlian.italase.shared.ui.components.GLASS_RADIUS
import com.anlian.italase.shared.ui.components.GlassButton
import com.anlian.italase.shared.ui.components.GlassCard
import com.anlian.italase.shared.ui.components.ImagePickerLauncher
import com.anlian.italase.shared.ui.components.enter
import com.anlian.italase.shared.ui.components.painter
import com.anlian.italase.shared.ui.components.rememberAppear
import com.anlian.italase.shared.ui.components.rememberImagePickerLauncher
import com.anlian.italase.shared.ui.navigation.LocalBottomOverlayPadding
import com.anlian.italase.shared.ui.navigation.LocalTopAppBarScrollBehavior
import com.anlian.italase.shared.ui.navigation.LocalTopOverlayPadding
import com.anlian.italase.shared.ui.navigation.nestedScrollConnection
import com.anlian.italase.shared.ui.navigation.TopBarInfoEffect
import com.anlian.italase.shared.ui.theme.Marigold
import com.anlian.italase.shared.ui.theme.MarigoldSoft
import com.anlian.italase.shared.ui.theme.WarungGreen
import com.anlian.italase.shared.ui.theme.WarungGreenSoft

/**
 * Layar Setup QRIS (AUD-107).
 *
 * Mengintegrasikan [rememberImagePickerLauncher] untuk kamera & galeri,
 * serta menampilkan dialog edukasi izin kamera jika akses ditolak pengguna.
 */
@Composable
fun QrisSetupScreen(
    onImageObtained: (ByteArray) -> Unit,
    onBack: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    var showPermissionDialog by rememberSaveable { mutableStateOf(false) }

    val launcher: ImagePickerLauncher = rememberImagePickerLauncher(
        onImageReady = { rawBytes ->
            onImageObtained(rawBytes)
        },
        onPermissionDenied = {
            showPermissionDialog = true
        },
    )

    TopBarInfoEffect(
        eyebrow = "PEMBAYARAN",
        title = "Setup QRIS",
    )

    QrisSetupContent(
        onCameraClick = { launcher.launchCamera() },
        onGalleryClick = { launcher.launchGallery() },
        modifier = modifier,
    )

    if (showPermissionDialog) {
        CameraPermissionEduDialog(
            onDismiss = { showPermissionDialog = false },
            onRetry = {
                showPermissionDialog = false
                launcher.launchCamera()
            },
        )
    }
}

/**
 * Konten visual Setup QRIS yang mendukung pratinjau Compose dan responsive layout.
 */
@Composable
fun QrisSetupContent(
    onCameraClick: () -> Unit,
    onGalleryClick: () -> Unit,
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

            // Header kartu penjelasan ringkas & elegan
            GlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .enter(appeared, 0),
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            AppIcon.PAYMENTS.painter(),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(22.dp),
                        )
                    }
                    Column(Modifier.weight(1f)) {
                        Text(
                            text = "Barcode Pembayaran Toko",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = "Pelanggan dapat scan barcode langsung di meja kasir. Pilih cara menambahkan barcode QRIS warung Anda:",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 18.sp,
                        )
                    }
                }
            }

            Spacer(Modifier.height(14.dp))

            if (isLandscape) {
                // Layout 2 Kolom untuk Lanskap
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    Box(Modifier.weight(1f)) {
                        SetupOptionCard(
                            title = "Ambil Foto Barcode",
                            description = "Buka kamera untuk mengambil foto stiker atau cetakan barcode QRIS warung Anda.",
                            badge = "Kamera Langsung",
                            badgeColor = WarungGreenSoft,
                            badgeTextColor = WarungGreen,
                            iconTint = WarungGreen,
                            onClick = onCameraClick,
                            modifier = Modifier.enter(appeared, 1),
                        )
                    }
                    Box(Modifier.weight(1f)) {
                        SetupOptionCard(
                            title = "Pilih dari Galeri",
                            description = "Pilih gambar barcode QRIS yang sudah tersimpan di galeri foto HP Anda.",
                            badge = "Galeri Foto",
                            badgeColor = MarigoldSoft,
                            badgeTextColor = Marigold,
                            iconTint = Marigold,
                            onClick = onGalleryClick,
                            modifier = Modifier.enter(appeared, 2),
                        )
                    }
                }
            } else {
                // Layout Vertikal untuk Potret
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    SetupOptionCard(
                        title = "Ambil Foto Barcode",
                        description = "Buka kamera untuk memotret stiker atau cetakan barcode QRIS warung Anda secara langsung.",
                        badge = "Kamera Langsung",
                        badgeColor = WarungGreenSoft,
                        badgeTextColor = WarungGreen,
                        iconTint = WarungGreen,
                        onClick = onCameraClick,
                        modifier = Modifier.enter(appeared, 1),
                    )

                    SetupOptionCard(
                        title = "Pilih dari Galeri",
                        description = "Pilih berkas gambar barcode QRIS yang sudah ada di galeri foto HP Anda.",
                        badge = "Galeri Foto",
                        badgeColor = MarigoldSoft,
                        badgeTextColor = Marigold,
                        iconTint = Marigold,
                        onClick = onGalleryClick,
                        modifier = Modifier.enter(appeared, 2),
                    )
                }
            }

            Spacer(Modifier.height(LocalBottomOverlayPadding.current + (if (isLandscape) 14.dp else 24.dp)))
        }
    }
}

/**
 * Kartu interaktif pilihan setup bergaya Warung Liquid Glass.
 */
@Composable
private fun SetupOptionCard(
    title: String,
    description: String,
    badge: String,
    badgeColor: Color,
    badgeTextColor: Color,
    iconTint: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    GlassCard(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        onClick = onClick,
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier
                    .size(50.dp)
                    .clip(CircleShape)
                    .background(badgeColor),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    AppIcon.PAYMENTS.painter(),
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(26.dp),
                )
            }

            Spacer(Modifier.width(14.dp))

            Column(Modifier.weight(1f)) {
                Box(
                    Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(badgeColor)
                        .padding(horizontal = 7.dp, vertical = 2.dp),
                ) {
                    Text(
                        text = badge,
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                        fontWeight = FontWeight.Bold,
                        color = badgeTextColor,
                        maxLines = 1,
                        softWrap = false,
                    )
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(Modifier.height(3.dp))
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 18.sp,
                )
            }

            Icon(
                AppIcon.CHEVRON_RIGHT.painter(),
                contentDescription = "Buka",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 8.dp),
            )
        }
    }
}
/**
 * Dialog edukasi ramah ketika izin akses kamera ditolak pengguna.
 * Menggunakan Warung emerald palette yang menenangkan tanpa warna merah destruktif.
 */
@Composable
fun CameraPermissionEduDialog(
    onDismiss: () -> Unit,
    onRetry: () -> Unit,
) {
    AnnounceGlassDialogOpen()
    val scheme = MaterialTheme.colorScheme
    val isDark = scheme.surface.luminance() < 0.5f

    var shown by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { shown = true }
    val appear by animateFloatAsState(
        if (shown) 1f else 0f, spring(dampingRatio = 0.9f, stiffness = 320f),
        label = "eduAppear",
    )
    val scale by animateFloatAsState(
        if (shown) 1f else 0.96f, spring(dampingRatio = 0.82f, stiffness = 380f),
        label = "eduScale",
    )

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = true,
            usePlatformDefaultWidth = false,
        ),
    ) {
        Box(
            Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                Modifier
                    .padding(horizontal = 28.dp)
                    .widthIn(max = 380.dp)
                    .graphicsLayer {
                        alpha = appear
                        scaleX = scale
                        scaleY = scale
                    }
                    .clip(RoundedCornerShape(GLASS_RADIUS))
                    .background(scheme.surface.copy(alpha = if (isDark) 0.58f else 0.54f)),
            ) {
                Box {
                    // Specular sheen & veils
                    Box(
                        Modifier
                            .matchParentSize()
                            .background(
                                Brush.verticalGradient(
                                    0f to Color.White.copy(alpha = if (isDark) 0.14f else 0.22f),
                                    0.25f to Color.Transparent,
                                ),
                            ),
                    )

                    Column(
                        Modifier.padding(horizontal = 24.dp, vertical = 24.dp),
                        horizontalAlignment = Alignment.Start,
                    ) {
                        Box(
                            Modifier
                                .size(52.dp)
                                .clip(CircleShape)
                                .background(WarungGreenSoft),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                AppIcon.PAYMENTS.painter(),
                                contentDescription = null,
                                tint = WarungGreen,
                                modifier = Modifier.size(26.dp),
                            )
                        }

                        Spacer(Modifier.height(16.dp))

                        Text(
                            text = "IZIN AKSES KAMERA",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.4.sp,
                            color = scheme.primary,
                        )

                        Spacer(Modifier.height(4.dp))

                        Text(
                            text = "Akses Kamera Diperlukan",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = scheme.onSurface,
                        )

                        Spacer(Modifier.height(8.dp))

                        Text(
                            text = "iTalase membutuhkan izin kamera untuk memotret barcode QRIS toko Anda. Anda juga dapat memilih gambar barcode langsung dari Galeri tanpa izin kamera.",
                            style = MaterialTheme.typography.bodyMedium,
                            lineHeight = 22.sp,
                            color = scheme.onSurface.copy(alpha = 0.86f),
                        )

                        Spacer(Modifier.height(24.dp))

                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            GlassButton(
                                text = "Tutup",
                                onClick = onDismiss,
                                container = scheme.onSurface.copy(alpha = if (isDark) 0.16f else 0.12f),
                                content = scheme.onSurface,
                                elevation = 0.dp,
                                specular = 0.18f,
                                modifier = Modifier.weight(1f),
                            )

                            GlassButton(
                                text = "Coba Lagi",
                                onClick = onRetry,
                                container = scheme.primary,
                                content = scheme.onPrimary,
                                elevation = 8.dp,
                                specular = 0.30f,
                                modifier = Modifier.weight(1.2f),
                            )
                        }
                    }
                }
            }
        }
    }
}

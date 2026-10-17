package com.anlian.italase.shared.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import italase.shared.generated.resources.Res
import italase.shared.generated.resources.cat_all
import italase.shared.generated.resources.cd_exit_warning
import italase.shared.generated.resources.exit_cancel
import italase.shared.generated.resources.exit_confirm
import italase.shared.generated.resources.exit_eyebrow
import italase.shared.generated.resources.exit_msg
import italase.shared.generated.resources.exit_title
import org.jetbrains.compose.resources.stringResource

@Composable
fun ScreenHeader(title: String, subtitle: String) {
    Column(Modifier.fillMaxWidth().padding(bottom = 4.dp)) {
        Text(
            subtitle.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold,
        )
        Text(title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun SectionHeader(title: String, trailing: String = "") {
    Row(
        Modifier.fillMaxWidth().padding(top = 12.dp, bottom = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        if (trailing.isNotBlank()) {
            Text(trailing, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun EmptyState(text: String, hint: String = "") {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    ) {
        Row(Modifier.fillMaxWidth().padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(44.dp).clip(CircleShape)
                    .background(MaterialTheme.colorScheme.secondaryContainer),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    AppIcon.WARNING.painter(),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                )
            }
            Column(Modifier.padding(start = 14.dp)) {
                Text(text, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                if (hint.isNotBlank()) {
                    Text(
                        hint,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

// Lencana status dengan kontras terkontrol (pengganti AssistChip disabled
// yang warnanya dikalahkan state disabled).
@Composable
fun StatusChip(text: String, good: Boolean) {
    Surface(
        shape = CircleShape,
        color = if (good) MaterialTheme.colorScheme.primaryContainer
        else MaterialTheme.colorScheme.secondaryContainer,
    ) {
        Text(
            text,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = if (good) MaterialTheme.colorScheme.onPrimaryContainer
            else MaterialTheme.colorScheme.onSecondaryContainer,
        )
    }
}

// Kartu daftar: putih dengan garis tepi agar tegas di atas background krem.
@Composable
fun ListCard(
    modifier: Modifier = Modifier,
    tonal: Boolean = false,
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier),
        colors = if (tonal) {
            CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
        } else {
            CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest)
        },
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        content()
    }
}

@Composable
fun MoneyBig(amount: String, label: String, light: Boolean = false) {
    val labelColor = if (light) Color.White.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onSurfaceVariant
    val valueColor = if (light) Color.White else MaterialTheme.colorScheme.onSurface
    Column {
        Text(label.uppercase(), style = MaterialTheme.typography.labelSmall, color = labelColor, fontWeight = FontWeight.Bold)
        Text(amount, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = valueColor)
    }
}

// ════════════════════════════════════════════════════════════════════════
//  GLASS DIALOG — Liquid Glass × M3 Expressive
// ════════════════════════════════════════════════════════════════════════
//  PENTING (2Percobaan gagal sebelumnya, catat biar ga diulang):
//   • JANGAN pakai renderEffect/BlurEffect di node yang berisi konten dialog.
//     RenderEffect hanya memburam isi node itu sendiri → teks ikut buram.
//   • JANGANBlur modifier pada container dialog.
//     Modifier.blur juga hanya memburam children-nya sendiri → sama saja.
//   • Backdrop blur yang benar dilakukan di sisi LAIN: Scaffold dipanggil
//     dari AppNavHost dengan Modifier.blur, sedangkan dialog ini tetap memakai
//     android.ui.window.Dialog (jendela terpisah) sehingga tidak pernah ikut
//     di-blur. Lihat AppNavHost.
//
//  Prinsip Liquid Glass / M3 Expressive yang dipakai di sini:
//   • TANPA border stroke. Edge glass muncul dari selisih luminance veil.
//   • Translucency nyata (alpha 0.52) di atas backdrop yang sudah buram.
//   • Tanpa gradient dekoratif — hanya sorot warna brand + specular sheen.
//   • Satu radius kontinu 28dp (squircle), tanpa double-bezel.
//   • Spring entry, press-scale di tombol.
private val GLASS_RADIUS = 28.dp

/** Dialog konfirmasi keluar. Jendela terpisah → tidak pernah ikut di-blur. */
@Composable
fun ExitConfirmDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    val isDark = scheme.surface.luminance() < 0.5f

    // Spring entry: fade + scale 0.96 → 1.
    var shown by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { shown = true }
    val appear by animateFloatAsState(
        if (shown) 1f else 0f, spring(dampingRatio = 0.9f, stiffness = 320f),
        label = "exitAppear",
    )
    val scale by animateFloatAsState(
        if (shown) 1f else 0.96f, spring(dampingRatio = 0.82f, stiffness = 380f),
        label = "exitScale",
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
                    // Translucency glass di atas backdrop yang sudah di-blur
                    // oleh Scaffold → ini bahan baku glassmorphism.
                    .background(scheme.surface.copy(alpha = if (isDark) 0.58f else 0.54f)),
            ) {
            // Tidak ada border. Edge glass muncul dari veil luminance di bawah.
            Box {
                // Veil: sorot warna brand samar di sudut atas-kiri + bawah-kanan.
                // Tanpa stroke, inilah satu-satunya penanda tepi → terbaca glass.
                Box(
                    Modifier
                        .matchParentSize()
                        .background(
                            Brush.radialGradient(
                                center = Offset(0f, 0f),
                                radius = 900f,
                                colors = listOf(
                                    scheme.primary.copy(alpha = 0.14f),
                                    Color.Transparent,
                                ),
                            ),
                        ),
                )
                Box(
                    Modifier
                        .matchParentSize()
                        .background(
                            Brush.radialGradient(
                                center = Offset.Infinite,
                                radius = 900f,
                                colors = listOf(
                                    scheme.secondary.copy(alpha = 0.10f),
                                    Color.Transparent,
                                ),
                            ),
                        ),
                )
                // Specular sheen atas (cahaya dari atas, ciri kaca).
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
                    // Ikon: chip translucent, tanpa border stroke.
                    Box(
                        Modifier
                            .size(52.dp)
                            .clip(CircleShape)
                            .background(scheme.errorContainer.copy(alpha = 0.85f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            AppIcon.WARNING.painter(),
                            contentDescription = stringResource(Res.string.cd_exit_warning),
                            tint = scheme.onErrorContainer,
                            modifier = Modifier.size(24.dp),
                        )
                    }

                    Text(
                        stringResource(Res.string.exit_eyebrow),
                        modifier = Modifier.padding(top = 16.dp),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.4.sp,
                        color = scheme.error.copy(alpha = 0.85f),
                    )
                    Text(
                        stringResource(Res.string.exit_title),
                        modifier = Modifier.padding(top = 4.dp),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = (-0.3).sp,
                        color = scheme.onSurface,
                    )
                    Text(
                        stringResource(Res.string.exit_msg),
                        modifier = Modifier.padding(top = 8.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        lineHeight = 22.sp,
                        color = scheme.onSurfaceVariant,
                    )

                    val cancelI = remember { MutableInteractionSource() }
                    val confirmI = remember { MutableInteractionSource() }
                    val cancelP by cancelI.collectIsPressedAsState()
                    val confirmP by confirmI.collectIsPressedAsState()

                    Row(
                        Modifier.fillMaxWidth().padding(top = 22.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        // Batal — glass tombol: translucent, tanpa stroke.
                        GlassTextButton(
                            text = stringResource(Res.string.exit_cancel),
                            onClick = onDismiss,
                            interactionSource = cancelI,
                            pressed = cancelP,
                            container = scheme.onSurface.copy(alpha = if (isDark) 0.10f else 0.06f),
                            content = scheme.onSurface,
                            modifier = Modifier.weight(1f),
                        )
                        // Ya, keluar — glass destruktif: tint error translucent.
                        GlassTextButton(
                            text = stringResource(Res.string.exit_confirm),
                            onClick = onConfirm,
                            interactionSource = confirmI,
                            pressed = confirmP,
                            container = scheme.error.copy(alpha = 0.88f),
                            content = scheme.onError,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
        }
    }
    }
}

/** Tombol teks gaya glass: translucent, press-scale, tanpa border stroke. */
@Composable
private fun GlassTextButton(
    text: String,
    onClick: () -> Unit,
    interactionSource: MutableInteractionSource,
    pressed: Boolean,
    container: Color,
    content: Color,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier
            .height(48.dp)
            .clip(CircleShape)
            .background(container)
            .graphicsLayer {
                val s = if (pressed) 0.97f else 1f
                scaleX = s
                scaleY = s
            }
            .clickable(interactionSource, indication = null) { onClick() },
        contentAlignment = Alignment.Center,
    ) {
        Text(text, fontWeight = FontWeight.SemiBold, color = content, maxLines = 1)
    }
}


// ════════════════════════════════════════════════════════════════════════
//  GLASS SURFACE — untuk konten yang IKUT SCROLL
// ════════════════════════════════════════════════════════════════════════
//  Sengaja TIDAK memakai Modifier.blur / renderEffect. Aturan mobile-fluid-ui
//  2026: blur live hanya boleh di overlay statis (navbar, sheet diam, scrim).
//  Kartu yang bergerak di dalam container scroll akan me-raster ulang tiap
//  frame → frame budget jebol di HP mid-range. Jadi "kaca" di sini dibentuk
//  dari translucency + specular sheen + shadow bertint: seluruhnya komposit
//  statis, nol biaya per-frame.
//
//  Kaca = translucency, bukan stroke. Karena itu TIDAK ada BorderStroke:
//  garis tepi adalah penanda paling kentara sebuah kartu Material murah.
//
//  Kenapa alpha 0.88, bukan 0.54 seperti dialog? Dialog berdiri di atas backdrop
//  yang SUDAH di-blur 20dp → warnanya seragam, jadi alpha rendah tetap bersih.
//  Kartu tidak boleh di-blur (aturan fluid-ui: jangan blur di layer yang
//  scroll), jadi ia berdiri di atas background tajam multi-warna + seam. Di
//  alpha 0.54 background itu bocor ke dalam dan kartunya jadi "kaca kotor".
//  Jadi di sini alpha tinggi, dan kesan kaca datang dari specular sheen +
//  shadow diffuse + edge tint — bukan dari see-through background.
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(24.dp),
    tint: Color = MaterialTheme.colorScheme.surface,
    alpha: Float = 0.88f,
    onClick: (() -> Unit)? = null,
    content: @Composable BoxScope.() -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    val isDark = scheme.surface.luminance() < 0.5f
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()

    Box(
        modifier
            .graphicsLayer {
                val s = if (pressed) 0.975f else 1f
                scaleX = s
                scaleY = s
            }
            .shadow(
                elevation = 22.dp,
                shape = shape,
                ambientColor = scheme.onSurface.copy(alpha = 0.12f),
                spotColor = scheme.onSurface.copy(alpha = 0.20f),
            )
            .clip(shape)
            .background(tint.copy(alpha = alpha))
            .then(
                if (onClick != null) {
                    Modifier.clickable(interaction, indication = null) { onClick() }
                } else {
                    Modifier
                },
            ),
    ) {
        // Edge glass — sorot warna brand di sudut, lalu specular sheen atas.
        // Sama persis bahasa visualnya dengan ExitConfirmDialog.
        Box(
            Modifier
                .matchParentSize()
                .background(
                    Brush.radialGradient(
                        center = Offset(0f, 0f),
                        radius = 900f,
                        colors = listOf(scheme.primary.copy(alpha = 0.05f), Color.Transparent),
                    ),
                ),
        )
        Box(
            Modifier
                .matchParentSize()
                .background(
                    Brush.verticalGradient(
                        0f to Color.White.copy(alpha = if (isDark) 0.14f else 0.34f),
                        0.30f to Color.Transparent,
                    ),
                ),
        )
        content()
    }
}

/** Empty state bergaya glass. Varian aditif — [EmptyState] lama tidak diubah. */
@Composable
fun GlassEmptyState(text: String, hint: String = "") {
    val scheme = MaterialTheme.colorScheme
    GlassCard(shape = RoundedCornerShape(20.dp)) {
        Row(Modifier.fillMaxWidth().padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(44.dp).clip(CircleShape).background(scheme.secondaryContainer),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    AppIcon.WARNING.painter(),
                    contentDescription = null,
                    tint = scheme.onSecondaryContainer,
                )
            }
            Column(Modifier.padding(start = 14.dp)) {
                Text(text, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                if (hint.isNotBlank()) {
                    Text(
                        hint,
                        style = MaterialTheme.typography.bodySmall,
                        color = scheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
fun CategoryChips(
    categories: List<String>,
    active: String?,
    onSelect: (String?) -> Unit,
) {
    if (categories.isEmpty()) return
    LazyRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item(key = "cat_all") {
            FilterChip(
                selected = active == null,
                onClick = { onSelect(null) },
                label = { Text(stringResource(Res.string.cat_all)) },
            )
        }
        items(categories, key = { "cat_$it" }) { c ->
            FilterChip(
                selected = active == c,
                onClick = { onSelect(if (active == c) null else c) },
                label = { Text(c) },
            )
        }
    }
}

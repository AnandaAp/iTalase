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
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
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
// Token radius kaca. Dibuka (bukan private) supaya layar bisa memakainya:
// aturan nesting Liquid Glass — radius kontainer HARUS lebih besar dari radius
// anaknya (kartu 28dp vs pill 24dp), kalau sama tepinya terlihat terjepit.
val GLASS_RADIUS = 28.dp

/**
 * RESEP KACA TUNGGAL — satu-satunya tempat gaya glass didefinisikan.
 * Dipakai oleh: kartu, baris list, chip, tombol, field, panel bottom bar, dialog.
 *
 * Urutan layer (sama persis dengan ExitConfirmDialog):
 *   shadow diffuse → press-scale → clip → tint → veil brand → specular sheen
 *
 * Digambar lewat drawBehind, BUKAN node Box `matchParentSize` terpisah: satu
 * draw pass, nol node layout tambahan, dan bisa dipakai modifier apa pun
 * (kartu, panel tab bar, field) tanpa wrapper.
 *
 * TIDAK ada BorderStroke. Tepi kaca terbentuk dari sheen (cahaya dari atas) +
 * veil brand — bukan garis seragam. Garis tepi adalah penanda paling kentara
 * sebuah kartu Material murah, dan tidak dipakai Liquid Glass maupun M3
 * Expressive. [specular] adalah penggantinya.
 *
 * @param alpha kekuatan panel. 0.54 hanya untuk permukaan di atas backdrop yang
 *   SUDAH DI-BLUR (lihat ExitConfirmDialog). Di atas konten tajam, nilai itu
 *   membuat kartu terlihat "kaca kotor" karena background bocor ke dalam —
 *   di situ pakai 0.80-0.88.
 * @param specular kekuatan sheen putih di tepi atas. Ini pengganti border.
 * @param veil sorot warna brand diagonal (kiri-atas primary, kanan-bawah
 *   secondary). Boleh mati untuk elemen kecil supaya tidak jadi norak.
 */
@Composable
fun Modifier.glassSurface(
    shape: Shape = RoundedCornerShape(GLASS_RADIUS),
    tint: Color = MaterialTheme.colorScheme.surface,
    alpha: Float = 0.88f,
    elevation: Dp = 22.dp,
    specular: Float = 0.34f,
    veil: Boolean = true,
    onClick: (() -> Unit)? = null,
): Modifier {
    val scheme = MaterialTheme.colorScheme
    val isDark = scheme.surface.luminance() < 0.5f
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()

    return this
        .graphicsLayer {
            val s = if (pressed) 0.975f else 1f
            scaleX = s
            scaleY = s
        }
        .shadow(
            elevation = elevation,
            shape = shape,
            ambientColor = scheme.onSurface.copy(alpha = 0.12f),
            spotColor = scheme.onSurface.copy(alpha = 0.20f),
        )
        .clip(shape)
        .drawBehind {
            drawRect(tint, alpha = alpha)
            if (veil) {
                drawRect(
                    Brush.radialGradient(
                        colors = listOf(scheme.primary.copy(alpha = 0.06f), Color.Transparent),
                        center = Offset.Zero,
                        radius = 900f,
                    ),
                )
                drawRect(
                    Brush.radialGradient(
                        colors = listOf(scheme.secondary.copy(alpha = 0.04f), Color.Transparent),
                        center = Offset(size.width, size.height),
                        radius = 900f,
                    ),
                )
            }
            // Specular: cahaya dari atas. Inilah "rim lit lip" yang bikin tepi
            // terbaca sebagai kaca tanpa perlu stroke apa pun.
            drawRect(
                Brush.verticalGradient(
                    0f to Color.White.copy(alpha = if (isDark) specular * 0.4f else specular),
                    0.28f to Color.Transparent,
                ),
            )
        }
        .then(
            if (onClick != null) {
                Modifier.clickable(interaction, indication = null) { onClick() }
            } else {
                Modifier
            },
        )
}

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

                    Row(
                        Modifier.fillMaxWidth().padding(top = 22.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        // Batal — glass tombol: translucent, tanpa stroke.
                        GlassButton(
                            text = stringResource(Res.string.exit_cancel),
                            onClick = onDismiss,
                            container = scheme.onSurface.copy(alpha = if (isDark) 0.10f else 0.06f),
                            content = scheme.onSurface,
                            modifier = Modifier.weight(1f),
                        )
                        // Ya, keluar — glass destruktif: tint error translucent.
                        GlassButton(
                            text = stringResource(Res.string.exit_confirm),
                            onClick = onConfirm,
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
    Box(
        modifier.glassSurface(shape = shape, tint = tint, alpha = alpha, onClick = onClick),
        content = content,
    )
}

/**
 * Baris list bergaya kaca — pengganti [ListCard] (yang memakai BorderStroke).
 * Dipakai layar Kasir, Stok, Bon, Riwayat, dan Lainnya.
 *
 * @param tint pakai `primaryContainer` untuk baris yang sedang aktif/terpilih.
 */
@Composable
fun GlassRow(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String = "",
    leading: (@Composable () -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
    tint: Color = MaterialTheme.colorScheme.surface,
    alpha: Float = 0.88f,
    onClick: (() -> Unit)? = null,
) {
    val scheme = MaterialTheme.colorScheme
    Row(
        modifier
            .fillMaxWidth()
            .glassSurface(
                shape = RoundedCornerShape(20.dp),
                tint = tint,
                alpha = alpha,
                elevation = 14.dp,
                specular = 0.28f,
                onClick = onClick,
            )
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (leading != null) {
            leading()
            Spacer(Modifier.width(12.dp))
        }
        Column(Modifier.weight(1f)) {
            Text(
                title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = scheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (subtitle.isNotBlank()) {
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = scheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        if (trailing != null) {
            Spacer(Modifier.width(12.dp))
            trailing()
        }
    }
}

/**
 * Field input bergaya kaca — pengganti langsung `OutlinedTextField` mentah.
 * Border Material dimatikan (semua warna border Transparent); tepi datang dari
 * [glassSurface], supaya satu bahasa visual dengan komponen lain.
 */
@Composable
fun GlassTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    leadingIcon: (@Composable () -> Unit)? = null,
) {
    val scheme = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(18.dp)
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        leadingIcon = leadingIcon,
        singleLine = true,
        shape = shape,
        modifier = modifier
            .fillMaxWidth()
            .glassSurface(shape = shape, alpha = 0.80f, elevation = 10.dp, specular = 0.22f),
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = Color.Transparent,
            unfocusedContainerColor = Color.Transparent,
            focusedBorderColor = Color.Transparent,
            unfocusedBorderColor = Color.Transparent,
            disabledBorderColor = Color.Transparent,
            cursorColor = scheme.primary,
        ),
    )
}

/** Chip pilihan bergaya kaca — pengganti `FilterChip` Material. */
@Composable
fun GlassChip(text: String, selected: Boolean, onClick: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    Box(
        Modifier
            .glassSurface(
                shape = CircleShape,
                tint = if (selected) scheme.primaryContainer else scheme.surface,
                alpha = if (selected) 0.95f else 0.78f,
                elevation = if (selected) 8.dp else 3.dp,
                specular = if (selected) 0.34f else 0.20f,
                veil = false,
                onClick = onClick,
            )
            .padding(horizontal = 16.dp, vertical = 9.dp),
    ) {
        Text(
            text,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            color = if (selected) scheme.onPrimaryContainer else scheme.onSurfaceVariant,
            maxLines = 1,
        )
    }
}

/**
 * Tombol pil bergaya kaca.
 *
 * DISABLED — dua aturan, keduanya dari pengukuran, bukan selera:
 *
 * 1. JANGAN memudarkan LABEL. Memudarkan teks di atas fill yang juga dipudarkan
 *    mendarat di mid-tone-di-atas-mid-tone. Di kartu hijau, semua kombinasi
 *    redup cuma mencapai 3.1:1 — tidak ada yang lolos AA. Jadi: yang meredup
 *    adalah FILL, label tetap full strength.
 *
 * 2. Warna disabled harus tergantung permukaan tempatnya duduk:
 *    • permukaan TERANG (baris/kartu putih) → fill netral [surfaceContainerHigh]
 *      + teks gelap. Fill brand yang dipudarkan menyisakan teks putih di atas
 *      bidang pucat (terukur 1.62:1), dan sisa hijaunya terbaca "tersedia".
 *    • permukaan GELAP (kartu brand) → fill lebih GELAP (recessed), bukan lebih
 *      terang. Di sana fill terang justru bertabrakan dengan state enabled:
 *      disabled #E2D7BE vs enabled sand #FDEBC8 cuma beda 1.217:1.
 *      Pakai [disabledContainer] / [disabledContent] untuk override.
 *
 * Padding horizontal WAJIB ada di sini. Tanpa itu lebar pill = lebar teks:
 * label panjang menempel di tepi melengkung, dan label pendek seperti "Habis"
 * pada tinggi 42dp berubah jadi LINGKARAN dengan teks meluber keluar.
 *
 * @param elevation bayangan. 0.dp untuk varian ghost (tint tipis di atas panel
 *   gelap) supaya tidak ada bayangan mengambang tanpa isi.
 * @param specular kekuatan gloss putih di tepi atas. Gloss HANYA terbaca di
 *   atas bidang gelap — di atas mint/putih ia hilang. 0f untuk tombol sekunder
 *   supaya hanya satu elemen yang menyala.
 * @param disabledContainer override fill saat disabled. null = netral terang.
 * @param disabledContent override label saat disabled. null = teks gelap.
 */
@Composable
fun GlassButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    container: Color = MaterialTheme.colorScheme.primary,
    content: Color = MaterialTheme.colorScheme.onPrimary,
    enabled: Boolean = true,
    height: Dp = 48.dp,
    elevation: Dp = 10.dp,
    specular: Float = 0.30f,
    disabledContainer: Color? = null,
    disabledContent: Color? = null,
) {
    val scheme = MaterialTheme.colorScheme
    Box(
        modifier
            .height(height)
            .glassSurface(
                shape = CircleShape,
                tint = if (enabled) container
                else disabledContainer ?: scheme.surfaceContainerHigh,
                alpha = if (enabled) 0.94f else 0.92f,
                elevation = if (enabled) elevation else 0.dp,
                specular = if (enabled) specular else 0f,
                veil = false,
                onClick = if (enabled) onClick else null,
            )
            .padding(horizontal = 20.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text,
            fontWeight = FontWeight.Bold,
            color = if (enabled) content
            else disabledContent ?: scheme.onSurface.copy(alpha = 0.72f),
            maxLines = 1,
        )
    }
}

/**
 * Tombol pil ikon bergaya kaca (minus/plus, aksi kecil).
 *
 * Fill default = brand pekat. Fill putih TIDAK bisa dipakai di atas kartu/baris
 * yang terang: terukur cuma 1.17:1 terhadap baris mint, jadi batas kontrolnya
 * tak terlihat sama sekali dan tombolnya terbaca sebagai glyph melayang.
 * Dengan primary: bidangnya 5.43:1 vs mint, ikonnya 6.52:1.
 */
@Composable
fun GlassIconButton(
    icon: AppIcon,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    size: Dp = 42.dp,
    container: Color = MaterialTheme.colorScheme.primary,
    content: Color = MaterialTheme.colorScheme.onPrimary,
) {
    val scheme = MaterialTheme.colorScheme
    Box(
        modifier
            .size(size)
            .glassSurface(
                shape = CircleShape,
                tint = if (enabled) container else scheme.surfaceContainerHigh,
                alpha = if (enabled) 0.94f else 0.92f,
                elevation = if (enabled) 6.dp else 0.dp,
                // Gloss mati saat disabled — tombol mati tidak boleh menyala.
                specular = if (enabled) 0.28f else 0f,
                veil = false,
                onClick = if (enabled) onClick else null,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            icon.painter(),
            contentDescription = contentDescription,
            tint = if (enabled) content else scheme.onSurface.copy(alpha = 0.72f),
            modifier = Modifier.size(size * 0.44f),
        )
    }
}

/**
 * FAB aksi-utama. SATU-SATUNYA FAB di app ini — warnanya sengaja tidak bisa
 * di-setel per layar, karena begitu satu layar memasang `containerColor` sendiri
 * tema universalnya bocor. Di app ini FAB mint default material3 nyaris tidak
 * terlihat di atas halaman krem, dan mint juga tidak bisa menampilkan gloss
 * putih (putih di atas near-white tetap near-white).
 *
 * Pakai ini bersama `Modifier.aboveTabBar()` supaya posisinya juga seragam.
 */
@Composable
fun PrimaryFab(
    text: String,
    icon: AppIcon,
    onClick: () -> Unit,
    contentDescription: String,
    modifier: Modifier = Modifier,
) {
    ExtendedFloatingActionButton(
        text = { Text(text, fontWeight = FontWeight.Bold) },
        icon = {
            Icon(
                icon.painter(),
                contentDescription = contentDescription,
                modifier = Modifier.size(20.dp),
            )
        },
        onClick = onClick,
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.primary,
        contentColor = MaterialTheme.colorScheme.onPrimary,
    )
}

/**
 * Shell dialog bergaya kaca (radius + veil + sheen yang sama dengan kartu).
 *
 * Catatan alpha: PayDialog TIDAK punya backdrop blur seperti ExitConfirmDialog
 * (blur itu dipasang AppNavHost di Scaffold hanya untuk konfirmasi keluar), jadi
 * alpha di sini 0.86 — bukan 0.54. Di atas konten tajam, 0.54 membuat isi dialog
 * bocor dan teksnya susah dibaca.
 */
@Composable
fun GlassDialog(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = true,
            usePlatformDefaultWidth = false,
        ),
    ) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(
                modifier
                    .padding(horizontal = 28.dp)
                    .widthIn(max = 380.dp)
                    .glassSurface(shape = RoundedCornerShape(GLASS_RADIUS), alpha = 0.86f)
                    .padding(horizontal = 20.dp, vertical = 20.dp),
                content = content,
            )
        }
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
            GlassChip(
                text = stringResource(Res.string.cat_all),
                selected = active == null,
                onClick = { onSelect(null) },
            )
        }
        items(categories, key = { "cat_$it" }) { c ->
            GlassChip(
                text = c,
                selected = active == c,
                onClick = { onSelect(if (active == c) null else c) },
            )
        }
    }
}

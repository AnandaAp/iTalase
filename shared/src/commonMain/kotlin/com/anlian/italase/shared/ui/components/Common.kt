package com.anlian.italase.shared.ui.components

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
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
import com.anlian.italase.shared.util.endOfListReached
import italase.shared.generated.resources.Res
import italase.shared.generated.resources.action_cancel
import italase.shared.generated.resources.cat_all
import italase.shared.generated.resources.cd_confirm_warning
import italase.shared.generated.resources.exit_cancel
import italase.shared.generated.resources.exit_confirm
import italase.shared.generated.resources.exit_eyebrow
import italase.shared.generated.resources.exit_msg
import italase.shared.generated.resources.exit_title
import italase.shared.generated.resources.load_more_loading
import org.jetbrains.compose.resources.stringResource

/**
 * Judul seksi dengan teks opsional di kanan (jumlah item / aksi "lihat semua").
 * Jarak atas-bawah sudah termasuk supaya ritme antar-seksi seragam.
 *
 * @param trailing teks kanan; kosong berarti tidak dirender.
 */
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

/**
 * Kartu "belum ada data" versi netral (non-kaca). Varian kaca: [GlassEmptyState].
 *
 * @param hint baris kedua opsional; kosong berarti tidak dirender.
 */
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
/**
 * Lencana status; warna dipilih eksplisit dari [good].
 *
 * @param good true → skema sukses (primaryContainer), false → netral
 *   (secondaryContainer).
 */
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

// ════════════════════════════════════════════════════════════════════════
//  LAZY LOAD — kaki daftar + pemicu halaman berikutnya
// ════════════════════════════════════════════════════════════════════════
//  Tinggi kaki dipesan TETAP walau sedang tidak memuat. Kalau tingginya
//  berubah-ubah, daftar yang sedang di-scroll akan tersentak saat indikator
//  muncul/hilang.
private val LOAD_MORE_FOOTER_HEIGHT = 56.dp

/**
 * Kaki daftar: indikator saat halaman berikutnya sedang dimuat.
 *
 * @param loading true → spinner + label; false → hanya memesan ruang kosong
 *   setinggi [LOAD_MORE_FOOTER_HEIGHT] supaya daftar tidak tersentak.
 */
@Composable
fun LoadMoreFooter(loading: Boolean, modifier: Modifier = Modifier) {
    Box(
        modifier.fillMaxWidth().height(LOAD_MORE_FOOTER_HEIGHT),
        contentAlignment = Alignment.Center,
    ) {
        if (!loading) return@Box
        Row(verticalAlignment = Alignment.CenterVertically) {
            CircularProgressIndicator(
                modifier = Modifier.size(18.dp),
                strokeWidth = 2.dp,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                stringResource(Res.string.load_more_loading),
                modifier = Modifier.padding(start = 10.dp),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/**
 * Panggil [onLoadMore] begitu daftar sampai di ujung bawah.
 *
 * Dipasang di dalam LazyColumn-nya sendiri; [enabled] dipakai untuk mematikan
 * pemicunya selama masih memuat atau saat sudah tidak ada halaman tersisa,
 * sehingga tidak ada permintaan beruntun.
 *
 * @param buffer berapa item dari ujung yang sudah dihitung "hampir sampai";
 *   lebih besar = pemicu lebih awal.
 */
@Composable
fun LazyListState.OnBottomReached(
    enabled: Boolean,
    onLoadMore: () -> Unit,
    buffer: Int = 3,
) {
    val reached by remember {
        derivedStateOf {
            endOfListReached(
                lastVisibleIndex = layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: -1,
                totalCount = layoutInfo.totalItemsCount,
                buffer = buffer,
            )
        }
    }
    LaunchedEffect(reached, enabled) {
        if (reached && enabled) onLoadMore()
    }
}

// ════════════════════════════════════════════════════════════════════════
//  MOTION — animasi masuk yang dipakai SEMUA layar
// ════════════════════════════════════════════════════════════════════════
//  Satu bahasa gerak untuk seluruh app: fade + naik 28dp, 420ms, decelerate,
//  bertahap 70ms per blok. Dipakai lewat [Enter] (pembungkus) atau
//  [Modifier.enter] kalau node-nya tidak boleh dibungkus — anak ber-`weight(1f)`
//  atau node ber-`align` akan kehilangan parent data-nya kalau dibungkus Box.

/**
 * Flag "layar sudah tampil". Satu per layar.
 * `remember` di dalam destinasi bertahan saat tab berpindah, jadi animasinya
 * main sekali per komposisi, bukan setiap kembali ke tab.
 */
@Stable
@Composable
fun rememberAppear(): Boolean {
    var appeared by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { appeared = true }
    return appeared
}

/**
 * Animasi masuk sebagai pembungkus. Jangan pakai untuk anak ber-weight/align —
 * di situ pakai [Modifier.enter] langsung, kalau tidak parent data-nya hilang.
 *
 * @param index urutan blok dalam layar (0,1,2…) untuk delay bertahap 70ms.
 */
@Composable
fun Enter(
    visible: Boolean,
    index: Int,
    content: @Composable () -> Unit,
) {
    Box(Modifier.enter(visible, index)) { content() }
}

/**
 * Animasi masuk sebagai modifier — untuk node yang tidak boleh dibungkus:
 * anak `weight(1f)` di Column/Row, atau node ber-`align` di Box.
 *
 * @param index urutan blok dalam layar (0,1,2…) untuk delay bertahap 70ms.
 */
@Composable
fun Modifier.enter(visible: Boolean, index: Int): Modifier {
    val progress by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(
            durationMillis = 420,
            delayMillis = index * 70,
            // Decelerate: cepat di awal, melambat halus di akhir.
            easing = CubicBezierEasing(0.16f, 1f, 0.3f, 1f),
        ),
        label = "enter$index",
    )
    return this.graphicsLayer {
        alpha = progress
        translationY = (1f - progress) * 28.dp.toPx()
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

    val primaryVeilBrush = if (veil) {
        remember(scheme.primary) {
            Brush.radialGradient(
                colors = listOf(scheme.primary.copy(alpha = 0.06f), Color.Transparent),
                center = Offset.Zero,
                radius = 900f,
            )
        }
    } else null

    val secondaryVeilColors = if (veil) {
        remember(scheme.secondary) {
            listOf(scheme.secondary.copy(alpha = 0.04f), Color.Transparent)
        }
    } else null

    val specularAlpha = if (isDark) specular * 0.4f else specular
    val specularBrush = remember(specularAlpha) {
        Brush.verticalGradient(
            0f to Color.White.copy(alpha = specularAlpha),
            0.28f to Color.Transparent,
        )
    }

    val clickModifier = if (onClick != null) {
        val interaction = remember { MutableInteractionSource() }
        val pressed by interaction.collectIsPressedAsState()
        Modifier
            .graphicsLayer {
                val s = if (pressed) 0.975f else 1f
                scaleX = s
                scaleY = s
            }
            .clickable(interaction, indication = null) { onClick() }
    } else {
        Modifier
    }

    return this
        .then(clickModifier)
        .shadow(
            elevation = elevation,
            shape = shape,
            ambientColor = scheme.onSurface.copy(alpha = 0.12f),
            spotColor = scheme.onSurface.copy(alpha = 0.20f),
        )
        .clip(shape)
        .drawBehind {
            drawRect(tint, alpha = alpha)
            if (primaryVeilBrush != null && secondaryVeilColors != null) {
                drawRect(primaryVeilBrush)
                drawRect(
                    Brush.radialGradient(
                        colors = secondaryVeilColors,
                        center = Offset(size.width, size.height),
                        radius = 900f,
                    ),
                )
            }
            // Specular: cahaya dari atas. Inilah "rim lit lip" yang bikin tepi
            // terbaca sebagai kaca tanpa perlu stroke apa pun.
            drawRect(specularBrush)
        }
}

// ════════════════════════════════════════════════════════════════════════
//  BACKDROP DIALOG KACA — siapa yang mem-blur konten app
// ════════════════════════════════════════════════════════════════════════
//  Masalahnya struktural: dialog Compose adalah JENDELA TERPISAH, jadi ia
//  tidak bisa mem-blur konten di belakangnya sendiri. Yang bisa mem-blur adalah
//  AppNavHost, lewat Modifier.blur pada subtree Scaffold.
//
//  Sebelumnya AppNavHost hanya mem-blur kalau `confirmExit` menyala — yaitu
//  HANYA dialog keluar dari tombol back. Dialog konfirmasi dari baris daftar
//  (hapus barang) tidak pernah memicunya, sehingga latarnya tetap TAJAM dan
//  teks latar menembus panel translusen 0.54 sampai menabrak teks dialog.
//  Akibatnya satu resep dialog yang sama terlihat berbeda tergantung dari mana
//  ia dipicu — persis keluhan "background tidak segelap waktu exit pop up".
//
//  Jadi penanda ini adalah SATU sumber untuk "ada dialog kaca terbuka", dibaca
//  AppNavHost dan ditulis oleh dialog mana pun. Sengaja bukan CompositionLocal:
//  nilainya global dan tunggal (satu jendela backdrop), sementara membungkus
//  seluruh body AppNavHost dengan provider berarti menggeser indentasi ~280
//  baris demi satu boolean. Kalau nanti ada dua dialog kaca yang boleh terbuka
//  bersamaan, ini yang harus jadi counter, bukan boolean.
/**
 * Penanda tunggal "ada dialog kaca terbuka". Ditulis oleh
 * [AnnounceGlassDialogOpen], dibaca AppNavHost untuk memutuskan blur Scaffold.
 * Lihat blok di atas untuk alasan kenapa bukan CompositionLocal.
 */
internal object GlassDialogBackdrop {
    var open: Boolean by mutableStateOf(false)
}

/** Dipasang di dalam setiap dialog kaca; menandai dan melepas sendiri. */
@Composable
internal fun AnnounceGlassDialogOpen() {
    DisposableEffect(Unit) {
        GlassDialogBackdrop.open = true
        onDispose { GlassDialogBackdrop.open = false }
    }
}

/**
 * Dialog konfirmasi bergaya kaca — SATU-SATUNYA resep dialog konfirmasi di app.
 *
 * Dipakai "Keluar aplikasi" dan "Hapus barang". Kalau resep ini disalin ke dua
 * tempat, cepat atau lambat keduanya berbeda dan user melihat dua bahasa visual
 * untuk satu jenis momen yang sama. Panel (veil + sheen + tombol kaca) tinggal
 * di sini; pemanggil hanya mengisi teks — lihat [ExitConfirmDialog].
 *
 * Jendela terpisah dari tree screen → tidak pernah ikut di-blur, dan boleh
 * memakai alpha panel 0.54 karena backdrop di belakangnya SUDAH di-blur Scaffold.
 *
 * @param eyebrow label kecil di atas judul; null → tidak dirender (jarak judul
 *   menyesuaikan otomatis).
 * @param warningDescription deskripsi a11y untuk ikon peringatan.
 */
@Composable
fun GlassConfirmDialog(
    title: String,
    message: String,
    confirmText: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    eyebrow: String? = null,
    cancelText: String = stringResource(Res.string.action_cancel),
    warningDescription: String = stringResource(Res.string.cd_confirm_warning),
) {
    // Konten app di belakang di-blur oleh AppNavHost selama dialog ini terbuka.
    AnnounceGlassDialogOpen()
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
                            contentDescription = warningDescription,
                            tint = scheme.onErrorContainer,
                            modifier = Modifier.size(24.dp),
                        )
                    }

                    eyebrow?.let {
                        Text(
                            it,
                            modifier = Modifier.padding(top = 16.dp),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.4.sp,
                            color = scheme.error.copy(alpha = 0.85f),
                        )
                    }
                    Text(
                        title,
                        modifier = Modifier.padding(top = if (eyebrow == null) 16.dp else 4.dp),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = (-0.3).sp,
                        color = scheme.onSurface,
                    )
                    Text(
                        message,
                        modifier = Modifier.padding(top = 8.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        lineHeight = 22.sp,
                        // Bukan onSurfaceVariant: panel dialog hanya 0.54 di atas
                        // latar yang di-blur+dim, jadi komposit warnanya tidak
                        // pernah seterang permukaan biasa. Baris ini yang wajib
                        // dibaca user sebelum aksi destruktif, jadi dipakai
                        // onSurface 0.86 → ±10:1 di panel, bukan ±5:1.
                        color = scheme.onSurface.copy(alpha = 0.86f),
                    )

                    Row(
                        Modifier.fillMaxWidth().padding(top = 22.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        // Batal — ghost: isi sangat tipis (0.06 cuma 1.12:1 terhadap
                        // panel, jadi tombolnya nyaris tak berbentuk). 0.12 → 1.28:1
                        // dan tetap terbaca sebagai pil kaca, bukan teks telanjang.
                        GlassButton(
                            text = cancelText,
                            onClick = onDismiss,
                            container = scheme.onSurface.copy(alpha = if (isDark) 0.16f else 0.12f),
                            content = scheme.onSurface,
                            elevation = 0.dp,
                            specular = 0.18f,
                            modifier = Modifier.weight(1f),
                        )
                        // Ya, keluar — destruktif TAPI tetap kaca. `error` pada 0.88
                        // praktis opak: ia jadi satu-satunya bidang pekat di dalam
                        // dialog yang serba translusen, sehingga "keluar dari tema".
                        // 0.80 menahan label putih di 4.63:1 (AA) sambil membiarkan
                        // panel menembus, dan sheen 0.30 terbaca di atas merah.
                        GlassButton(
                            text = confirmText,
                            onClick = onConfirm,
                            container = scheme.error.copy(alpha = 0.80f),
                            content = scheme.onError,
                            elevation = 8.dp,
                            specular = 0.30f,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
        }
    }
    }
}

/** Konfirmasi keluar aplikasi — teksnya saja yang khusus, panelnya milik [GlassConfirmDialog]. */
@Composable
fun ExitConfirmDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    GlassConfirmDialog(
        eyebrow = stringResource(Res.string.exit_eyebrow),
        title = stringResource(Res.string.exit_title),
        message = stringResource(Res.string.exit_msg),
        confirmText = stringResource(Res.string.exit_confirm),
        cancelText = stringResource(Res.string.exit_cancel),
        onConfirm = onConfirm,
        onDismiss = onDismiss,
    )
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
/**
 * Kartu kaca untuk konten yang ikut scroll. Pembungkus tipis [glassSurface] + Box.
 *
 * @param onClick non-null → seluruh kartu dapat ditekan; efek tekan datang dari
 *   press-scale [glassSurface], bukan ripple.
 */
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
 * Baris list bergaya kaca — komponen daftar standar app ini.
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

/**
 * Chip pilihan bergaya kaca — pengganti `FilterChip` Material.
 *
 * `onClick` sengaja parameter TERAKHIR walaupun `modifier` ditambahkan sesudahnya
 * — kalau `modifier` yang terakhir, trailing lambda di pemanggil lama akan
 * diam-diam mengikat ke `modifier` dan gagal dikompilasi dengan pesan yang
 * menyesatkan. Aturan Compose: trailing lambda = parameter terakhir.
 */
@Composable
fun GlassChip(
    text: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    Box(
        modifier
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
 *    • permukaan TERANG (baris/kartu putih) → fill netral `surfaceContainerHigh`
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
 * @param container warna fill saat enabled.
 * @param content warna label saat enabled.
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
 *
 * @param size diameter tombol; ikonnya digambar pada 44% nilai ini.
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
 * FAB aksi-utama bergaya Liquid Glass × Material3 Expressive × Glassmorphism.
 *
 * Mengikuti resep permukaan kaca tunggal [glassSurface]:
 *  - Surface translusen dengan warna brand primary (WarungGreen) dan alpha 0.92f
 *  - Elevasi mengambang 14dp dengan tinted shadow ambient (12%) dan spot (20%)
 *  - Specular sheen 0.42f (rim lit lip di tepi atas tanpa border stroke)
 *  - Veil sorot brand diagonal (primary di kiri-atas, marigold di kanan-bawah)
 *  - Bentuk pil (CircleShape) dengan tinggi standar M3 Expressive 56dp
 *  - Animasi tekan pegas (spring) yang taktil dan responsif (Apple Fluid Interfaces WWDC 2018)
 *  - Label bold dengan gaya M3 Expressive labelLarge
 *
 * Pakai ini bersama `Modifier.aboveTabBar()` supaya posisinya juga seragam di atas bottom tab bar.
 */
@Composable
fun PrimaryFab(
    text: String,
    icon: AppIcon,
    onClick: () -> Unit,
    contentDescription: String,
    modifier: Modifier = Modifier,
    expanded: Boolean = true,
    shape: Shape = CircleShape,
) {
    val scheme = MaterialTheme.colorScheme
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.93f else 1f,
        animationSpec = spring(
            dampingRatio = 0.75f,
            stiffness = 500f,
        ),
        label = "fabPressScale",
    )

    Box(
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .height(56.dp)
            .then(
                if (expanded) Modifier
                else Modifier.size(56.dp)
            )
            .glassSurface(
                shape = shape,
                tint = scheme.primary,
                alpha = 0.92f,
                elevation = 14.dp,
                specular = 0.42f,
                veil = true,
                onClick = null,
            )
            .clickable(
                interactionSource = interaction,
                indication = null,
                role = Role.Button,
                onClick = onClick,
            )
            .clearAndSetSemantics {
                this.role = Role.Button
                this.contentDescription = contentDescription
            }
            .padding(horizontal = if (expanded) 20.dp else 0.dp),
        contentAlignment = Alignment.Center,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Icon(
                icon.painter(),
                contentDescription = null,
                tint = scheme.onPrimary,
                modifier = Modifier.size(20.dp),
            )
            if (expanded) {
                Text(
                    text = text,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = scheme.onPrimary,
                    maxLines = 1,
                )
            }
        }
    }
}

/**
 * Shell dialog bergaya liquid glass dengan animasi masuk pegas (Apple Fluid Interfaces WWDC 2018).
 */
@Composable
fun GlassDialog(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    AnnounceGlassDialogOpen()
    val scheme = MaterialTheme.colorScheme

    // Spring entry: fade + scale 0.94 -> 1.0 (Apple WWDC Fluid Interfaces)
    var shown by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { shown = true }
    val appear by animateFloatAsState(
        targetValue = if (shown) 1f else 0f,
        animationSpec = spring(dampingRatio = 0.90f, stiffness = 340f),
        label = "glassDialogAppear",
    )
    val scale by animateFloatAsState(
        targetValue = if (shown) 1f else 0.94f,
        animationSpec = spring(dampingRatio = 0.82f, stiffness = 380f),
        label = "glassDialogScale",
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
            Modifier
                .fillMaxSize()
                .imePadding(),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                modifier
                    .padding(horizontal = 24.dp, vertical = 24.dp)
                    .widthIn(max = 420.dp)
                    .graphicsLayer {
                        alpha = appear
                        scaleX = scale
                        scaleY = scale
                    }
                    .glassSurface(
                        shape = RoundedCornerShape(GLASS_RADIUS),
                        tint = scheme.surface,
                        alpha = 0.88f,
                        elevation = 14.dp,
                        specular = 0.28f,
                    )
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

/**
 * Baris chip kategori horizontal. Chip "Semua" selalu dirender di depan dan
 * mewakili filter kosong (null).
 *
 * @param active kategori terpilih; null = "Semua". Menekan chip yang sudah
 *   aktif mengembalikan ke "Semua".
 * @param onSelect nilai null dikirim saat "Semua" dipilih.
 */
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

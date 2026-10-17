package com.anlian.italase.shared.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import italase.shared.generated.resources.Res
import italase.shared.generated.resources.noto_sans_bold
import italase.shared.generated.resources.noto_sans_medium
import italase.shared.generated.resources.noto_sans_regular
import italase.shared.generated.resources.noto_sans_semibold
import org.jetbrains.compose.resources.Font

// --- Warung design tokens (single source of truth) ---
// Cream paper background + deep warung green + marigold accent.
// Body text on cream and white text on primary both pass WCAG AA.

val WarungGreen = Color(0xFF1D6B3C)
val WarungGreenDark = Color(0xFF0E3D20)
val WarungGreenSoft = Color(0xFFDCEFE1)
val Marigold = Color(0xFFE8930C)
val MarigoldSoft = Color(0xFFFDEBC8)
val MarigoldInk = Color(0xFF5C3800)
val Paper = Color(0xFFFAF6EE)
val CardWhite = Color(0xFFFFFDF7)
val SandLow = Color(0xFFF3EDDE)
val Sand = Color(0xFFEAE1CC)
val InkSoft = Color(0xFF6B6250)

internal val LightColors = lightColorScheme(
    primary = WarungGreen,
    onPrimary = Color.White,
    primaryContainer = WarungGreenSoft,
    onPrimaryContainer = WarungGreenDark,
    secondary = Marigold,
    onSecondary = Color.White,
    secondaryContainer = MarigoldSoft,
    onSecondaryContainer = MarigoldInk,
    tertiary = Color(0xFF0E7C86),
    background = Paper,
    onBackground = Color(0xFF1C1B17),
    surface = CardWhite,
    onSurface = Color(0xFF1C1B17),
    surfaceVariant = SandLow,
    onSurfaceVariant = InkSoft,
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = SandLow,
    surfaceContainer = Sand,
    surfaceContainerHigh = Color(0xFFE2D7BE),
    outlineVariant = Color(0xFFD8CFB6),
)

internal val DarkColors = darkColorScheme(
    primary = Color(0xFF7BD89A),
    onPrimary = Color(0xFF06351A),
    primaryContainer = WarungGreenDark,
    onPrimaryContainer = Color(0xFFDCEFE1),
    secondary = Marigold,
    secondaryContainer = Color(0xFF4A2E00),
    onSecondaryContainer = MarigoldSoft,
    background = Color(0xFF14120C),
    surface = Color(0xFF1E1B14),
    surfaceContainerLow = Color(0xFF242117),
    surfaceContainer = Color(0xFF2B271C),
    surfaceContainerHigh = Color(0xFF333022),
)

private val WarungShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(28.dp),
)

/**
 * Noto Sans — satu-satunya keluarga huruf app.
 *
 * Dibangun DI DALAM composable karena `org.jetbrains.compose.resources.Font`
 * adalah fungsi @Composable: resource font Compose Multiplatform dibaca lewat
 * komposisi, jadi FontFamily-nya tidak bisa jadi `val` di level file seperti
 * warna.
 *
 * Empat berkas statis, bukan satu berkas variable font. Variable font belum
 * tentu menerapkan sumbu `wght` dengan benar di mesin grafis iOS, sementara
 * Compose memilih dari daftar `Font(res, weight)` ini secara deterministik di
 * semua target. Ongkosnya +2,5 MB APK; kalau itu jadi masalah, subset Latin
 * dulu (fonttools) lalu kurangi beratnya.
 */
@Composable
private fun notoSans(): FontFamily = FontFamily(
    Font(Res.font.noto_sans_regular, FontWeight.Normal),
    Font(Res.font.noto_sans_medium, FontWeight.Medium),
    Font(Res.font.noto_sans_semibold, FontWeight.SemiBold),
    Font(Res.font.noto_sans_bold, FontWeight.Bold),
)

/**
 * Tempelkan keluarga huruf ke SELURUH 30 gaya Material3 (15 baku + 15
 * `*Emphasized`).
 *
 * Versi Material3 yang dipakai proyek ini (1.12.0-alpha03) TIDAK punya
 * `Typography(defaultFontFamily = ...)` — konstruktornya hanya menerima TextStyle
 * eksplisit, jadi jalur satu baris itu tidak tersedia dan gayanya harus dipetakan
 * satu per satu. Gaya `*Emphasized` ikut dipetakan walaupun app ini tidak
 * menyebutnya langsung: komponen M3 Expressive membacanya dari typography ini.
 */
private fun withFontFamily(base: Typography, f: FontFamily): Typography = base.copy(
    displayLarge = base.displayLarge.copy(fontFamily = f),
    displayMedium = base.displayMedium.copy(fontFamily = f),
    displaySmall = base.displaySmall.copy(fontFamily = f),
    headlineLarge = base.headlineLarge.copy(fontFamily = f),
    headlineMedium = base.headlineMedium.copy(fontFamily = f),
    headlineSmall = base.headlineSmall.copy(fontFamily = f),
    titleLarge = base.titleLarge.copy(fontFamily = f),
    titleMedium = base.titleMedium.copy(fontFamily = f),
    titleSmall = base.titleSmall.copy(fontFamily = f),
    bodyLarge = base.bodyLarge.copy(fontFamily = f),
    bodyMedium = base.bodyMedium.copy(fontFamily = f),
    bodySmall = base.bodySmall.copy(fontFamily = f),
    labelLarge = base.labelLarge.copy(fontFamily = f),
    labelMedium = base.labelMedium.copy(fontFamily = f),
    labelSmall = base.labelSmall.copy(fontFamily = f),
    displayLargeEmphasized = base.displayLargeEmphasized.copy(fontFamily = f),
    displayMediumEmphasized = base.displayMediumEmphasized.copy(fontFamily = f),
    displaySmallEmphasized = base.displaySmallEmphasized.copy(fontFamily = f),
    headlineLargeEmphasized = base.headlineLargeEmphasized.copy(fontFamily = f),
    headlineMediumEmphasized = base.headlineMediumEmphasized.copy(fontFamily = f),
    headlineSmallEmphasized = base.headlineSmallEmphasized.copy(fontFamily = f),
    titleLargeEmphasized = base.titleLargeEmphasized.copy(fontFamily = f),
    titleMediumEmphasized = base.titleMediumEmphasized.copy(fontFamily = f),
    titleSmallEmphasized = base.titleSmallEmphasized.copy(fontFamily = f),
    bodyLargeEmphasized = base.bodyLargeEmphasized.copy(fontFamily = f),
    bodyMediumEmphasized = base.bodyMediumEmphasized.copy(fontFamily = f),
    bodySmallEmphasized = base.bodySmallEmphasized.copy(fontFamily = f),
    labelLargeEmphasized = base.labelLargeEmphasized.copy(fontFamily = f),
    labelMediumEmphasized = base.labelMediumEmphasized.copy(fontFamily = f),
    labelSmallEmphasized = base.labelSmallEmphasized.copy(fontFamily = f),
)

@Composable
fun ITalaseTheme(dark: Boolean = false, content: @Composable () -> Unit) {
    val fontFamily = notoSans()
    MaterialTheme(
        colorScheme = AppColorScheme(dark),
        shapes = WarungShapes,
        typography = withFontFamily(Typography(), fontFamily),
    ) {
        // Komponen Material3 sudah mewarisi huruf dari typography di atas, TAPI
        // `Text(...)` tanpa `style` membaca LocalTextStyle — dan nilai
        // defaultnya TextStyle.Default, yang tidak punya keluarga huruf sama
        // sekali. Tanpa baris ini, semua teks polos (justru mayoritas di app
        // ini) tetap memakai huruf sistem dan hasilnya campur-campur.
        //
        // Sengaja HANYA fontFamily yang di-set. Menyetel bodyMedium utuh akan
        // ikut memaksa ukuran + line-height ke setiap Text polos dan menggeser
        // tata letak yang sudah disetujui. Aman di-merge karena M3 memakai
        // `LocalTextStyle.current.merge(style)`, jadi atribut lain tetap apa
        // adanya.
        CompositionLocalProvider(
            LocalTextStyle provides TextStyle(fontFamily = fontFamily),
        ) {
            content()
        }
    }
}

// Brand scheme on iOS / older Android, Material You dynamic scheme on Android 12+.
@Composable
expect fun AppColorScheme(dark: Boolean): ColorScheme

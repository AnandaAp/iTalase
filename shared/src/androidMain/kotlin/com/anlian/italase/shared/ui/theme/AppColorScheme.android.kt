package com.anlian.italase.shared.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.Composable

// Brand scheme everywhere. Material You dynamicColor nge-skip: tiap device
// dapat palet berbeda sehingga dialog/bottom bar kehilangan identitas warung
// (terlihat generic Google). Konsisten di semua device = kesan premium.
@Composable
actual fun AppColorScheme(dark: Boolean): ColorScheme = if (dark) DarkColors else LightColors

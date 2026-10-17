package com.anlian.italase.shared.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.Composable

/**
 * Skema warna aplikasi: `dark` memilih salah satu palet tetap, karena iOS tidak punya padanan
 * dynamic color Material.
 */
@Composable
actual fun AppColorScheme(dark: Boolean): ColorScheme = if (dark) DarkColors else LightColors

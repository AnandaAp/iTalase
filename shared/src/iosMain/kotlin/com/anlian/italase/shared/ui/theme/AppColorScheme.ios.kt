package com.anlian.italase.shared.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.Composable

@Composable
actual fun AppColorScheme(dark: Boolean): ColorScheme = if (dark) DarkColors else LightColors

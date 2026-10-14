package com.anlian.italase.shared.ui.theme

import android.os.Build
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

@Composable
actual fun AppColorScheme(dark: Boolean): ColorScheme {
    if (!dark && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        return dynamicLightColorScheme(LocalContext.current)
    }
    return if (dark) DarkColors else LightColors
}

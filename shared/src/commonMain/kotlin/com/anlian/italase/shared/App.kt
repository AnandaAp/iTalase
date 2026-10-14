package com.anlian.italase.shared

import androidx.compose.runtime.Composable
import com.anlian.italase.shared.ui.navigation.AppNavHost
import com.anlian.italase.shared.ui.theme.ITalaseTheme
import com.anlian.italase.shared.ui.theme.WarungBackground

@Composable
fun App() {
    ITalaseTheme {
        WarungBackground {
            AppNavHost()
        }
    }
}

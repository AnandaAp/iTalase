package com.anlian.italase.shared

import androidx.compose.runtime.Composable
import com.anlian.italase.shared.ui.navigation.AppNavHost
import com.anlian.italase.shared.ui.theme.ITalaseTheme
import com.anlian.italase.shared.ui.theme.WarungBackground

/**
 * Root composable yang dipakai bersama Android dan iOS.
 *
 * Urutannya bukan pilihan gaya: [WarungBackground] membaca warna dari
 * MaterialTheme (harus di dalam [ITalaseTheme]), dan layar harus dirender DI
 * DALAM background supaya kontennya menggulir di atas blob, bukan di bawahnya.
 */
@Composable
fun App() {
    ITalaseTheme {
        WarungBackground {
            AppNavHost()
        }
    }
}

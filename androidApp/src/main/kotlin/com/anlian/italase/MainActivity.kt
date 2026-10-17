package com.anlian.italase

import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.view.WindowCompat
import com.anlian.italase.shared.App

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Edge-to-edge dengan scrim NOL di kedua system bar.
        //
        // Penting: enableEdgeToEdge() tanpa argumen memakai SystemBarStyle.auto()
        // yang MEMASANG SCRIM pada navigation bar (khususnya di mode 3-tombol).
        // Scrim itulah strip putih yang menutupi background app di bar bawah.
        // Dengan scrim transparan, background app menembus sampai ke system bar.
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.auto(
                lightScrim = Color.TRANSPARENT,
                darkScrim = Color.TRANSPARENT,
            ),
            navigationBarStyle = SystemBarStyle.auto(
                lightScrim = Color.TRANSPARENT,
                darkScrim = Color.TRANSPARENT,
            ),
        )
        // Allow content to resize when soft keyboard appears
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
            window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
        }
        // Transparent system bars (YouTube-style edge-to-edge)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        val controller = WindowCompat.getInsetsController(window, window.decorView)
        controller.isAppearanceLightStatusBars = true
        controller.isAppearanceLightNavigationBars = true
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.VANILLA_ICE_CREAM) {
            window.statusBarColor = Color.TRANSPARENT
            window.navigationBarColor = Color.TRANSPARENT
        }
        // Android 10+ memasang scrim kontras otomatis di belakang ikon system
        // bar. Matikan di level API juga, bukan hanya lewat atribut theme —
        // beberapa OEM mengabaikan atribut theme-nya.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.isNavigationBarContrastEnforced = false
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.VANILLA_ICE_CREAM) {
                window.isStatusBarContrastEnforced = false
            }
        }
        setContent {
            App()
        }
    }
}

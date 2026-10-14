package com.anlian.italase

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.view.WindowCompat
import com.anlian.italase.shared.App
import com.anlian.italase.shared.di.AndroidAppContext
import com.anlian.italase.shared.di.initKoin

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        // Transparent system bars (YouTube-style edge-to-edge)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        val controller = WindowCompat.getInsetsController(window, window.decorView)
        controller.isAppearanceLightStatusBars = true
        controller.isAppearanceLightNavigationBars = true
        window.statusBarColor = android.graphics.Color.TRANSPARENT
        window.navigationBarColor = android.graphics.Color.TRANSPARENT
        AndroidAppContext.ctx = applicationContext
        initKoin()
        setContent {
            App()
        }
    }
}

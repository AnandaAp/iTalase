package com.anlian.italase.shared

import androidx.compose.ui.window.ComposeUIViewController
import com.anlian.italase.shared.di.initKoin
import org.koin.core.annotation.KoinViewModelScopeApi
import org.koin.core.option.viewModelScopeFactory
import platform.UIKit.UIViewController

/**
 * Titik masuk iOS: dipanggil dari Swift untuk membungkus [App] dalam
 * UIViewController milik Compose.
 *
 * Koin di-init di `configure` — callback itu jalan sebelum komposisi pertama,
 * jadi ViewModel sudah bisa di-resolve saat layar pertama dirender.
 */
@OptIn(KoinViewModelScopeApi::class)
fun MainViewController(): UIViewController = ComposeUIViewController(
    configure = { initKoin {
        options(viewModelScopeFactory())
    } },
) { App() }

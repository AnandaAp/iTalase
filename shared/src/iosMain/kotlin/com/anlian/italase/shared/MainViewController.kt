package com.anlian.italase.shared

import androidx.compose.ui.window.ComposeUIViewController
import com.anlian.italase.shared.di.initKoin
import platform.UIKit.UIViewController

fun MainViewController(): UIViewController = ComposeUIViewController(
    configure = { initKoin() },
) { App() }

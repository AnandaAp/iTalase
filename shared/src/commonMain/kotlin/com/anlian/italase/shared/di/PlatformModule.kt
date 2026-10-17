package com.anlian.italase.shared.di

import org.koin.core.annotation.Module
import org.koin.core.component.KoinComponent

/**
 * Modul Koin yang isinya berbeda per platform: Android menyediakan `Context`,
 * iOS tidak.
 *
 * Konkretnya ada di `PlatformModule.android.kt` dan `PlatformModule.ios.kt`.
 * Turunan [KoinComponent] diperlukan supaya modulnya boleh meminta dependensi
 * milik platform itu sendiri saat menyusun graf.
 */
@Module
expect class PlatformModule: KoinComponent

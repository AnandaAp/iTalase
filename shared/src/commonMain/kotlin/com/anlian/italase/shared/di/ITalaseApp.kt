package com.anlian.italase.shared.di

import org.koin.core.annotation.KoinApplication
import org.koin.dsl.KoinAppDeclaration
import org.koin.plugin.module.dsl.startKoin

/**
 * Akar graf Koin.
 *
 * `@KoinApplication` mengikat [SharedModule] (hasil scan seluruh kode bersama)
 * dengan [PlatformModule] (implementasi per-platform). Objeknya sengaja kosong:
 * daftar modulnya dihasilkan KSP dari anotasi ini, bukan ditulis tangan.
 */
@KoinApplication(modules = [SharedModule::class, PlatformModule::class])
object ITalaseApp

/** Koin hanya boleh di-start sekali per proses; [initKoin] memakai ini sebagai penjaga. */
private var koinStarted: Boolean = false

/**
 * Menyalakan Koin dengan deklarasi tambahan milik platform (Android mengoper
 * `androidContext`).
 *
 * Aman dipanggil berkali-kali: panggilan berikutnya diabaikan. Tanpa penjaga itu
 * preview Compose dan `Application` bisa saling menimpa graf dependensi.
 */
fun initKoin(additional: KoinAppDeclaration = {}) {
    if (koinStarted) return
    startKoin<ITalaseApp>(additional)
    koinStarted = true
}

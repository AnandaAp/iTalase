package com.anlian.italase.shared.di

import org.koin.core.annotation.KoinApplication
import org.koin.plugin.module.dsl.startKoin

@KoinApplication(modules = [SharedModule::class, PlatformModule::class])
object ITalaseApp

private var koinStarted: Boolean = false

fun initKoin() {
    if (koinStarted) return
    startKoin<ITalaseApp>()
    koinStarted = true
}
